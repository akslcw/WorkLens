package com.su.worklens_backend.service.impl;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.EmployeeReportResponse;
import com.su.worklens_backend.dto.UsageRecordResponse;
import com.su.worklens_backend.service.EmployeeReportService;
import com.su.worklens_backend.service.LlmProvider;
import com.su.worklens_backend.service.ReportHistoryService;
import com.su.worklens_backend.service.UsageRecordService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmployeeReportServiceImpl implements EmployeeReportService {

    private static final int REPORT_WINDOW_DAYS = 7;

    private final UsageRecordService usageRecordService;
    private final LlmProvider llmProvider;
    private final ReportHistoryService reportHistoryService;

    public EmployeeReportServiceImpl(UsageRecordService usageRecordService, LlmProvider llmProvider, ReportHistoryService reportHistoryService) {
        this.usageRecordService = usageRecordService;
        this.llmProvider = llmProvider;
        this.reportHistoryService = reportHistoryService;
    }

    @Override
    public EmployeeReportResponse generateWeeklyReport(AuthenticatedUser authenticatedUser) {
        LocalDateTime reportEnd = LocalDateTime.now();
        LocalDateTime reportStart = reportEnd.minusDays(REPORT_WINDOW_DAYS);
        List<UsageRecordResponse> recentRecords = usageRecordService.listUsageRecords(authenticatedUser).stream()
                .filter(record -> !record.getStartedAt().isBefore(reportStart) && !record.getStartedAt().isAfter(reportEnd))
                .sorted(Comparator.comparing(UsageRecordResponse::getStartedAt).thenComparing(UsageRecordResponse::getId))
                .toList();

        String prompt = buildPrompt(authenticatedUser, reportStart, reportEnd, recentRecords);
        String summary = llmProvider.generateText(prompt);
        reportHistoryService.saveEmployeeWeeklyReport(authenticatedUser.getEmployeeId(), reportStart, reportEnd, summary);
        return new EmployeeReportResponse(summary);
    }

    private String buildPrompt(
            AuthenticatedUser authenticatedUser,
            LocalDateTime reportStart,
            LocalDateTime reportEnd,
            List<UsageRecordResponse> recentRecords
    ) {
        long totalUsageMinutes = recentRecords.stream()
                .mapToLong(this::calculateUsageMinutes)
                .sum();

        Map<String, Long> usageMinutesByApp = recentRecords.stream()
                .collect(Collectors.groupingBy(
                        UsageRecordResponse::getAppName,
                        Collectors.summingLong(this::calculateUsageMinutes)
                ));

        String appSummary = usageMinutesByApp.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> entry.getKey() + ": " + entry.getValue() + " minutes")
                .collect(Collectors.joining("\n"));

        String recordLines = recentRecords.stream()
                .map(record -> "%s | %s -> %s".formatted(
                        record.getAppName(),
                        record.getStartedAt(),
                        record.getEndedAt()
                ))
                .collect(Collectors.joining("\n"));

        if (appSummary.isBlank()) {
            appSummary = "No records in the last 7 days.";
        }
        if (recordLines.isBlank()) {
            recordLines = "No detailed records in the last 7 days.";
        }

        return """
                You are writing an encouraging weekly personal productivity summary for a WorkLens employee.
                Write the report in Chinese by default.
                Keep the tone supportive, practical, and concise.
                Return plain text only.
                Do not use Markdown, bullet syntax, headings, bold markers, tables, or code fences.
                Do not mention any other employees or team-level comparisons.

                Employee username: %s
                Reporting window: %s to %s
                Total usage minutes: %d

                App summary:
                %s

                Detailed records from the last 7 days:
                %s
                """.formatted(
                authenticatedUser.getUsername(),
                reportStart,
                reportEnd,
                totalUsageMinutes,
                appSummary,
                recordLines
        );
    }

    private long calculateUsageMinutes(UsageRecordResponse record) {
        return Duration.between(record.getStartedAt(), record.getEndedAt()).toMinutes();
    }
}
