package com.su.worklens_backend.service.impl;

import com.su.worklens_backend.service.EmployeeDailyReportArchiveRequest;
import com.su.worklens_backend.service.ReportArchiveService;
import com.su.worklens_backend.service.TeamDailyReportArchiveRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Service
public class ReportArchiveServiceImpl implements ReportArchiveService {

    private static final String EMPLOYEE_SCOPE = "EMPLOYEE";
    private static final String TEAM_SCOPE = "TEAM";
    private static final String DAILY_PERIOD = "DAILY";
    private static final String RAW_USAGE_SOURCE = "RAW_USAGE";

    private final JdbcTemplate jdbcTemplate;

    public ReportArchiveServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void archiveDailyReports(List<EmployeeDailyReportArchiveRequest> employeeReports, TeamDailyReportArchiveRequest teamReport) {
        List<Long> sourceRecordIds = new ArrayList<>();
        for (EmployeeDailyReportArchiveRequest report : employeeReports) {
            insertEmployeeDailyReport(report);
            sourceRecordIds.addAll(report.sourceRecordIds());
        }
        if (teamReport != null) {
            insertTeamDailyReport(teamReport);
        }
        deleteSourceRecords(sourceRecordIds);
    }

    @Override
    @Transactional
    public void archiveEmployeeDailyReports(List<EmployeeDailyReportArchiveRequest> reports) {
        archiveDailyReports(reports, null);
    }

    @Override
    @Transactional
    public void archiveEmployeeDailyReport(
            Long employeeId,
            LocalDate reportDate,
            LocalDateTime periodStartedAt,
            LocalDateTime periodEndedAt,
            String detailJson,
            String summary,
            int sourceCount,
            List<Long> sourceRecordIds
    ) {
        archiveEmployeeDailyReports(List.of(new EmployeeDailyReportArchiveRequest(
                employeeId,
                reportDate,
                periodStartedAt,
                periodEndedAt,
                detailJson,
                summary,
                sourceCount,
                sourceRecordIds
        )));
    }

    private void insertEmployeeDailyReport(EmployeeDailyReportArchiveRequest report) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO llm_reports (
                            report_type,
                            requester_employee_id,
                            target_employee_id,
                            summary,
                            period_started_at,
                            period_ended_at,
                            created_at,
                            report_scope,
                            period_type,
                            period_start_date,
                            period_end_date,
                            detail_json,
                            source_layer,
                            source_count,
                            generated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                        """,
                "EMPLOYEE_DAILY",
                report.employeeId(),
                report.employeeId(),
                report.summary(),
                Timestamp.valueOf(report.periodStartedAt()),
                Timestamp.valueOf(report.periodEndedAt()),
                Timestamp.valueOf(now),
                EMPLOYEE_SCOPE,
                DAILY_PERIOD,
                report.reportDate(),
                report.reportDate(),
                report.detailJson(),
                RAW_USAGE_SOURCE,
                report.sourceCount(),
                Timestamp.valueOf(now)
        );
    }

    private void insertTeamDailyReport(TeamDailyReportArchiveRequest report) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO llm_reports (
                            report_type,
                            requester_employee_id,
                            target_employee_id,
                            summary,
                            period_started_at,
                            period_ended_at,
                            created_at,
                            report_scope,
                            period_type,
                            period_start_date,
                            period_end_date,
                            detail_json,
                            source_layer,
                            source_count,
                            generated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                        """,
                "TEAM_DAILY",
                null,
                null,
                report.summary(),
                Timestamp.valueOf(report.periodStartedAt()),
                Timestamp.valueOf(report.periodEndedAt()),
                Timestamp.valueOf(now),
                TEAM_SCOPE,
                DAILY_PERIOD,
                report.reportDate(),
                report.reportDate(),
                report.detailJson(),
                RAW_USAGE_SOURCE,
                report.sourceCount(),
                Timestamp.valueOf(now)
        );
    }

    private void deleteSourceRecords(List<Long> sourceRecordIds) {
        if (sourceRecordIds.isEmpty()) {
            return;
        }

        StringJoiner placeholders = new StringJoiner(", ");
        List<Object> parameters = new ArrayList<>();
        for (Long sourceRecordId : sourceRecordIds) {
            placeholders.add("?");
            parameters.add(sourceRecordId);
        }

        jdbcTemplate.update(
                "DELETE FROM usage_records WHERE id IN (" + placeholders + ")",
                parameters.toArray()
        );
    }
}
