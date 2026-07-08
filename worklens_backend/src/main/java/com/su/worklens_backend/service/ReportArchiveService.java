package com.su.worklens_backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReportArchiveService {

    void archiveDailyReports(List<EmployeeDailyReportArchiveRequest> employeeReports, TeamDailyReportArchiveRequest teamReport);

    void archiveWeeklyReports(List<EmployeeWeeklyReportArchiveRequest> employeeReports, TeamWeeklyReportArchiveRequest teamReport);

    void archiveEmployeeDailyReports(List<EmployeeDailyReportArchiveRequest> reports);

    void archiveEmployeeDailyReport(
            Long employeeId,
            LocalDate reportDate,
            LocalDateTime periodStartedAt,
            LocalDateTime periodEndedAt,
            String detailJson,
            String summary,
            int sourceCount,
            List<Long> sourceRecordIds
    );
}
