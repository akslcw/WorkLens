package com.su.worklens_backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.su.worklens_backend.service.EmployeeDailyReportArchiveRequest;
import com.su.worklens_backend.service.LlmProvider;
import com.su.worklens_backend.service.ReportArchiveService;
import com.su.worklens_backend.service.ReportGenerationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportGenerationServiceImpl implements ReportGenerationService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final LlmProvider llmProvider;
    private final ReportArchiveService reportArchiveService;

    public ReportGenerationServiceImpl(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            LlmProvider llmProvider,
            ReportArchiveService reportArchiveService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.llmProvider = llmProvider;
        this.reportArchiveService = reportArchiveService;
    }

    @Override
    public void generateDailyReports(LocalDate reportDate) {
        LocalDateTime periodStartedAt = reportDate.atStartOfDay();
        LocalDateTime periodEndedAt = reportDate.plusDays(1).atStartOfDay();
        List<Long> employeeIds = findEmployeeIdsWithUsage(periodStartedAt, periodEndedAt);
        List<EmployeeDailyReportArchiveRequest> archiveRequests = new ArrayList<>();

        for (Long employeeId : employeeIds) {
            List<UsageRecordSnapshot> sourceRecords = findDailyUsageRecords(employeeId, periodStartedAt, periodEndedAt);
            if (sourceRecords.isEmpty()) {
                continue;
            }

            List<ReportDetailItem> detailItems = buildDetailItems(sourceRecords);
            String detailJson = toJson(detailItems);
            String summary = llmProvider.generateText(buildEmployeeDailyPrompt(employeeId, reportDate, detailItems));

            archiveRequests.add(new EmployeeDailyReportArchiveRequest(
                    employeeId,
                    reportDate,
                    periodStartedAt,
                    periodEndedAt,
                    detailJson,
                    summary,
                    sourceRecords.size(),
                    sourceRecords.stream().map(UsageRecordSnapshot::id).toList()
            ));
        }

        if (!archiveRequests.isEmpty()) {
            reportArchiveService.archiveEmployeeDailyReports(archiveRequests);
        }
    }

    private List<Long> findEmployeeIdsWithUsage(LocalDateTime periodStartedAt, LocalDateTime periodEndedAt) {
        return jdbcTemplate.queryForList(
                """
                        SELECT DISTINCT employee_id
                        FROM usage_records
                        WHERE started_at >= ? AND started_at < ?
                        ORDER BY employee_id
                        """,
                Long.class,
                Timestamp.valueOf(periodStartedAt),
                Timestamp.valueOf(periodEndedAt)
        );
    }

    private List<UsageRecordSnapshot> findDailyUsageRecords(Long employeeId, LocalDateTime periodStartedAt, LocalDateTime periodEndedAt) {
        return jdbcTemplate.query(
                """
                        SELECT id, app_name, started_at, ended_at
                        FROM usage_records
                        WHERE employee_id = ? AND started_at >= ? AND started_at < ?
                        ORDER BY started_at, id
                        """,
                this::toUsageRecordSnapshot,
                employeeId,
                Timestamp.valueOf(periodStartedAt),
                Timestamp.valueOf(periodEndedAt)
        );
    }

    private UsageRecordSnapshot toUsageRecordSnapshot(ResultSet resultSet, int rowNumber) throws SQLException {
        return new UsageRecordSnapshot(
                resultSet.getLong("id"),
                resultSet.getString("app_name"),
                resultSet.getTimestamp("started_at").toLocalDateTime(),
                resultSet.getTimestamp("ended_at").toLocalDateTime()
        );
    }

    private List<ReportDetailItem> buildDetailItems(List<UsageRecordSnapshot> sourceRecords) {
        Map<String, Long> durationSecondsByApp = sourceRecords.stream()
                .collect(Collectors.groupingBy(
                        UsageRecordSnapshot::appName,
                        LinkedHashMap::new,
                        Collectors.summingLong(this::calculateDurationSeconds)
                ));
        long totalDurationSeconds = durationSecondsByApp.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        return durationSecondsByApp.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new ReportDetailItem(
                        entry.getKey(),
                        entry.getValue(),
                        Math.round(entry.getValue() / 60.0),
                        calculateRatio(entry.getValue(), totalDurationSeconds)
                ))
                .toList();
    }

    private long calculateDurationSeconds(UsageRecordSnapshot sourceRecord) {
        return Math.max(0, Duration.between(sourceRecord.startedAt(), sourceRecord.endedAt()).getSeconds());
    }

    private BigDecimal calculateRatio(long durationSeconds, long totalDurationSeconds) {
        if (totalDurationSeconds == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(durationSeconds)
                .divide(BigDecimal.valueOf(totalDurationSeconds), 4, RoundingMode.HALF_UP);
    }

    private String toJson(List<ReportDetailItem> detailItems) {
        try {
            return objectMapper.writeValueAsString(detailItems);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize report detail JSON", exception);
        }
    }

    private String buildEmployeeDailyPrompt(Long employeeId, LocalDate reportDate, List<ReportDetailItem> detailItems) {
        String appSummary = detailItems.stream()
                .map(item -> "%s | durationSeconds=%d | ratio=%s".formatted(
                        item.appName(),
                        item.durationSeconds(),
                        item.ratio()
                ))
                .collect(Collectors.joining("\n"));
        if (appSummary.isBlank()) {
            appSummary = "No usage data is available.";
        }

        return """
                You are writing an encouraging daily personal productivity summary for a WorkLens employee.
                Write the report in Chinese by default.
                Keep the tone supportive, practical, and concise.
                Return plain text only.
                Do not use Markdown, bullet syntax, headings, bold markers, tables, or code fences.
                Do not mention any other employees or team-level comparisons.

                Employee id: %d
                Report date: %s

                Structured app usage:
                %s
                """.formatted(employeeId, reportDate, appSummary);
    }

    private record UsageRecordSnapshot(Long id, String appName, LocalDateTime startedAt, LocalDateTime endedAt) {
    }

    private record ReportDetailItem(String appName, long durationSeconds, long durationMinutes, BigDecimal ratio) {
    }
}
