package com.su.worklens_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.AppUsageRatioResponse;
import com.su.worklens_backend.dto.ReportDetailItemResponse;
import com.su.worklens_backend.dto.TeamUsageSummaryResponse;
import com.su.worklens_backend.dto.UsageAppCardResponse;
import com.su.worklens_backend.dto.UsageRecordRequest;
import com.su.worklens_backend.dto.UsageRecordResponse;
import com.su.worklens_backend.dto.UsageReportViewResponse;
import com.su.worklens_backend.dto.UsageSegmentResponse;
import com.su.worklens_backend.dto.UsageViewResponse;
import com.su.worklens_backend.entity.UsageRecord;
import com.su.worklens_backend.mapper.UsageRecordMapper;
import com.su.worklens_backend.service.UsageRecordService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsageRecordServiceImpl implements UsageRecordService {

    private static final long ADJACENT_RECORD_TOLERANCE_SECONDS = 15;

    private final UsageRecordMapper usageRecordMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public UsageRecordServiceImpl(UsageRecordMapper usageRecordMapper, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.usageRecordMapper = usageRecordMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<UsageRecordResponse> listUsageRecords(AuthenticatedUser authenticatedUser) {
        return usageRecordMapper.selectList(
                        new LambdaQueryWrapper<UsageRecord>()
                                .eq(UsageRecord::getEmployeeId, authenticatedUser.getEmployeeId())
                                .orderByAsc(UsageRecord::getStartedAt, UsageRecord::getId)
                ).stream()
                .map(this::toUsageRecordResponse)
                .toList();
    }

    @Override
    public UsageViewResponse getUsageView(AuthenticatedUser authenticatedUser, LocalDate date, int page, int pageSize) {
        int normalizedPage = Math.max(1, page);
        int normalizedPageSize = Math.min(10, Math.max(1, pageSize));
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime nextDayStart = date.plusDays(1).atStartOfDay();

        List<UsageRecord> rawRecords = usageRecordMapper.selectList(
                new LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getEmployeeId, authenticatedUser.getEmployeeId())
                        .ge(UsageRecord::getStartedAt, dayStart)
                        .lt(UsageRecord::getStartedAt, nextDayStart)
                        .orderByAsc(UsageRecord::getStartedAt, UsageRecord::getId)
        );

        if (!rawRecords.isEmpty()) {
            List<UsageAppCardResponse> cards = buildUsageAppCards(rawRecords);
            int fromIndex = Math.min((normalizedPage - 1) * normalizedPageSize, cards.size());
            int toIndex = Math.min(fromIndex + normalizedPageSize, cards.size());
            return UsageViewResponse.liveUsage(
                    date,
                    normalizedPage,
                    normalizedPageSize,
                    cards.size(),
                    cards.subList(fromIndex, toIndex)
            );
        }

        Optional<UsageReportViewResponse> report = findCoveringReport(authenticatedUser.getEmployeeId(), date);
        return UsageViewResponse.report(date, report.orElse(null));
    }

    @Override
    public UsageRecordResponse createUsageRecord(UsageRecordRequest request, AuthenticatedUser authenticatedUser) {
        UsageRecord latestRecord = usageRecordMapper.selectOne(
                new LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getEmployeeId, authenticatedUser.getEmployeeId())
                        .orderByDesc(UsageRecord::getEndedAt, UsageRecord::getId)
                        .last("LIMIT 1")
        );
        if (canMergeIntoLatestRecord(latestRecord, request)) {
            if (request.getEndedAt().isAfter(latestRecord.getEndedAt())) {
                latestRecord.setEndedAt(request.getEndedAt());
                usageRecordMapper.updateById(latestRecord);
            }
            return toUsageRecordResponse(latestRecord);
        }

        UsageRecord usageRecord = new UsageRecord();
        usageRecord.setEmployeeId(authenticatedUser.getEmployeeId());
        usageRecord.setAppName(request.getAppName().trim());
        usageRecord.setStartedAt(request.getStartedAt());
        usageRecord.setEndedAt(request.getEndedAt());
        usageRecord.setCreatedAt(LocalDateTime.now());
        usageRecordMapper.insert(usageRecord);

        return toUsageRecordResponse(usageRecord);
    }

    @Override
    public TeamUsageSummaryResponse getTeamUsageSummary() {
        List<UsageRecord> usageRecords = usageRecordMapper.selectList(null);
        if (usageRecords.isEmpty()) {
            return new TeamUsageSummaryResponse(BigDecimal.ZERO, 0L, 0, List.of());
        }

        long totalUsageMinutes = usageRecords.stream()
                .mapToLong(this::calculateUsageMinutes)
                .sum();

        int activeEmployeeCount = (int) usageRecords.stream()
                .map(UsageRecord::getEmployeeId)
                .distinct()
                .count();

        BigDecimal teamAverageUsageMinutes = activeEmployeeCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalUsageMinutes)
                .divide(BigDecimal.valueOf(activeEmployeeCount), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        Map<String, Long> usageMinutesByApp = usageRecords.stream()
                .collect(Collectors.groupingBy(
                        UsageRecord::getAppName,
                        LinkedHashMap::new,
                        Collectors.summingLong(this::calculateUsageMinutes)
                ));

        List<AppUsageRatioResponse> appUsageRatios = usageMinutesByApp.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new AppUsageRatioResponse(
                        entry.getKey(),
                        entry.getValue(),
                        BigDecimal.valueOf(entry.getValue())
                                .divide(BigDecimal.valueOf(totalUsageMinutes), 4, RoundingMode.HALF_UP)
                ))
                .toList();

        return new TeamUsageSummaryResponse(teamAverageUsageMinutes, totalUsageMinutes, activeEmployeeCount, appUsageRatios);
    }

    private long calculateUsageMinutes(UsageRecord usageRecord) {
        return Duration.between(usageRecord.getStartedAt(), usageRecord.getEndedAt()).toMinutes();
    }

    private List<UsageAppCardResponse> buildUsageAppCards(List<UsageRecord> rawRecords) {
        Map<String, List<UsageRecord>> recordsByApp = rawRecords.stream()
                .collect(Collectors.groupingBy(
                        UsageRecord::getAppName,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return recordsByApp.entrySet().stream()
                .map(entry -> {
                    List<UsageSegmentResponse> segments = mergeSegments(entry.getValue());
                    long durationSeconds = segments.stream()
                            .mapToLong(segment -> Duration.between(segment.getStartedAt(), segment.getEndedAt()).getSeconds())
                            .sum();
                    return new UsageAppCardResponse(entry.getKey(), durationSeconds, segments);
                })
                .sorted(Comparator.comparingLong(UsageAppCardResponse::getDurationSeconds).reversed()
                        .thenComparing(UsageAppCardResponse::getAppName))
                .toList();
    }

    private List<UsageSegmentResponse> mergeSegments(List<UsageRecord> records) {
        List<UsageSegmentResponse> segments = new ArrayList<>();
        for (UsageRecord record : records) {
            if (segments.isEmpty()) {
                segments.add(new UsageSegmentResponse(record.getStartedAt(), record.getEndedAt()));
                continue;
            }

            UsageSegmentResponse latestSegment = segments.get(segments.size() - 1);
            long gapSeconds = Duration.between(latestSegment.getEndedAt(), record.getStartedAt()).getSeconds();
            if (gapSeconds >= 0 && gapSeconds <= ADJACENT_RECORD_TOLERANCE_SECONDS) {
                if (record.getEndedAt().isAfter(latestSegment.getEndedAt())) {
                    latestSegment.setEndedAt(record.getEndedAt());
                }
            } else {
                segments.add(new UsageSegmentResponse(record.getStartedAt(), record.getEndedAt()));
            }
        }
        return segments;
    }

    private Optional<UsageReportViewResponse> findCoveringReport(Long employeeId, LocalDate date) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT report_scope, period_type, period_start_date, period_end_date, summary, detail_json::text AS detail_json
                        FROM llm_reports
                        WHERE report_scope = 'EMPLOYEE'
                          AND target_employee_id = ?
                          AND period_start_date <= ?
                          AND period_end_date >= ?
                        ORDER BY CASE period_type
                                     WHEN 'DAILY' THEN 1
                                     WHEN 'WEEKLY' THEN 2
                                     WHEN 'MONTHLY' THEN 3
                                     ELSE 4
                                 END,
                                 generated_at DESC,
                                 id DESC
                        LIMIT 1
                        """,
                employeeId,
                date,
                date
        );
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> row = rows.get(0);
        return Optional.of(new UsageReportViewResponse(
                row.get("report_scope").toString(),
                row.get("period_type").toString(),
                ((Date) row.get("period_start_date")).toLocalDate(),
                ((Date) row.get("period_end_date")).toLocalDate(),
                row.get("summary").toString(),
                parseReportDetails(row.get("detail_json").toString())
        ));
    }

    private List<ReportDetailItemResponse> parseReportDetails(String detailJson) {
        try {
            return objectMapper.readValue(detailJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse report detail JSON", exception);
        }
    }

    private boolean canMergeIntoLatestRecord(UsageRecord latestRecord, UsageRecordRequest request) {
        if (latestRecord == null) {
            return false;
        }
        if (!latestRecord.getAppName().equals(request.getAppName().trim())) {
            return false;
        }
        long gapSeconds = Duration.between(latestRecord.getEndedAt(), request.getStartedAt()).getSeconds();
        return gapSeconds >= 0 && gapSeconds <= ADJACENT_RECORD_TOLERANCE_SECONDS;
    }

    private UsageRecordResponse toUsageRecordResponse(UsageRecord usageRecord) {
        return new UsageRecordResponse(
                usageRecord.getId(),
                usageRecord.getAppName(),
                usageRecord.getStartedAt(),
                usageRecord.getEndedAt(),
                usageRecord.getCreatedAt()
        );
    }
}
