package com.su.worklens_backend.controller;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.EmployeeReportResponse;
import com.su.worklens_backend.dto.LlmTestResponse;
import com.su.worklens_backend.dto.ReportHistoryResponse;
import com.su.worklens_backend.dto.TeamReportResponse;
import com.su.worklens_backend.service.AuthService;
import com.su.worklens_backend.service.EmployeeReportService;
import com.su.worklens_backend.service.LlmProvider;
import com.su.worklens_backend.service.ReportHistoryService;
import com.su.worklens_backend.service.TeamReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LlmController {

    static final String TEST_PROMPT = "Please respond to this fixed WorkLens connectivity check text.";

    private final LlmProvider llmProvider;
    private final EmployeeReportService employeeReportService;
    private final TeamReportService teamReportService;
    private final ReportHistoryService reportHistoryService;
    private final AuthService authService;

    public LlmController(
            LlmProvider llmProvider,
            EmployeeReportService employeeReportService,
            TeamReportService teamReportService,
            ReportHistoryService reportHistoryService,
            AuthService authService
    ) {
        this.llmProvider = llmProvider;
        this.employeeReportService = employeeReportService;
        this.teamReportService = teamReportService;
        this.reportHistoryService = reportHistoryService;
        this.authService = authService;
    }

    @GetMapping("/llm/test-response")
    public LlmTestResponse getTestResponse() {
        return new LlmTestResponse(llmProvider.generateText(TEST_PROMPT));
    }

    @PostMapping("/llm/employee-report")
    public EmployeeReportResponse generateEmployeeReport(HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return employeeReportService.generateWeeklyReport(authenticatedUser);
    }

    @PostMapping("/llm/team-report")
    public TeamReportResponse generateTeamReport(HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return teamReportService.generateTeamReport(authenticatedUser);
    }

    @GetMapping("/llm/employee-report-history")
    public List<ReportHistoryResponse> getEmployeeReportHistory(HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return reportHistoryService.listEmployeeReportHistory(authenticatedUser.getEmployeeId());
    }

    @GetMapping("/llm/team-report-history")
    public List<ReportHistoryResponse> getTeamReportHistory(HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return reportHistoryService.listTeamReportHistory(authenticatedUser.getEmployeeId());
    }
}
