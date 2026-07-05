package com.su.worklens_backend.service.impl;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.AppUsageRatioResponse;
import com.su.worklens_backend.dto.TeamReportResponse;
import com.su.worklens_backend.dto.TeamUsageSummaryResponse;
import com.su.worklens_backend.service.LlmProvider;
import com.su.worklens_backend.service.ReportHistoryService;
import com.su.worklens_backend.service.TeamReportService;
import com.su.worklens_backend.service.UsageRecordService;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class TeamReportServiceImpl implements TeamReportService {

    private final UsageRecordService usageRecordService;
    private final LlmProvider llmProvider;
    private final ReportHistoryService reportHistoryService;

    public TeamReportServiceImpl(UsageRecordService usageRecordService, LlmProvider llmProvider, ReportHistoryService reportHistoryService) {
        this.usageRecordService = usageRecordService;
        this.llmProvider = llmProvider;
        this.reportHistoryService = reportHistoryService;
    }

    @Override
    public TeamReportResponse generateTeamReport(AuthenticatedUser authenticatedUser) {
        TeamUsageSummaryResponse summary = usageRecordService.getTeamUsageSummary();
        String prompt = buildPrompt(summary);
        String reportSummary = llmProvider.generateText(prompt);
        reportHistoryService.saveTeamSummaryReport(authenticatedUser.getEmployeeId(), reportSummary);
        return new TeamReportResponse(reportSummary);
    }

    private String buildPrompt(TeamUsageSummaryResponse summary) {
        String appSummary = summary.getAppUsageRatios().stream()
                .map(this::toAppSummaryLine)
                .collect(Collectors.joining("\n"));

        if (appSummary.isBlank()) {
            appSummary = "No aggregated app usage data is available.";
        }

        return """
                You are writing a concise team usage briefing for a WorkLens manager.
                Use only the aggregated metrics below.
                Do not invent or infer any individual employee detail.
                Do not mention names, employee identifiers, usernames, or raw activity records.

                teamAverageUsageMinutes: %s
                totalUsageMinutes: %d
                activeEmployeeCount: %d

                appUsageRatios:
                %s
                """.formatted(
                summary.getTeamAverageUsageMinutes(),
                summary.getTotalUsageMinutes(),
                summary.getActiveEmployeeCount(),
                appSummary
        );
    }

    private String toAppSummaryLine(AppUsageRatioResponse ratio) {
        return "- %s | usageMinutes=%d | usageRatio=%s".formatted(
                ratio.getAppName(),
                ratio.getUsageMinutes(),
                ratio.getUsageRatio()
        );
    }
}
