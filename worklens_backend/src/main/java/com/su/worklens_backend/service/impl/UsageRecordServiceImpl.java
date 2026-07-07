package com.su.worklens_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.AppUsageRatioResponse;
import com.su.worklens_backend.dto.TeamUsageSummaryResponse;
import com.su.worklens_backend.dto.UsageRecordRequest;
import com.su.worklens_backend.dto.UsageRecordResponse;
import com.su.worklens_backend.entity.UsageRecord;
import com.su.worklens_backend.mapper.UsageRecordMapper;
import com.su.worklens_backend.service.UsageRecordService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UsageRecordServiceImpl implements UsageRecordService {

    private static final long ADJACENT_RECORD_TOLERANCE_SECONDS = 15;

    private final UsageRecordMapper usageRecordMapper;

    public UsageRecordServiceImpl(UsageRecordMapper usageRecordMapper) {
        this.usageRecordMapper = usageRecordMapper;
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
