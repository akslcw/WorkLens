package com.su.worklens_backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EmployeeDailyReportArchiveRequest(
        Long employeeId,
        LocalDate reportDate,
        LocalDateTime periodStartedAt,
        LocalDateTime periodEndedAt,
        String detailJson,
        String summary,
        int sourceCount,
        List<Long> sourceRecordIds
) {
}
