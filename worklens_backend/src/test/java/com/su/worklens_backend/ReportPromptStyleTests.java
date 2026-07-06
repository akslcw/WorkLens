package com.su.worklens_backend;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.AppUsageRatioResponse;
import com.su.worklens_backend.dto.TeamUsageSummaryResponse;
import com.su.worklens_backend.dto.UsageRecordResponse;
import com.su.worklens_backend.service.impl.EmployeeReportServiceImpl;
import com.su.worklens_backend.service.impl.TeamReportServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportPromptStyleTests {

    @Test
    void employeeReportPromptRequiresChinesePlainTextWithoutMarkdown() throws Exception {
        EmployeeReportServiceImpl service = new EmployeeReportServiceImpl(null, null, null);
        Method buildPrompt = EmployeeReportServiceImpl.class.getDeclaredMethod(
                "buildPrompt",
                AuthenticatedUser.class,
                LocalDateTime.class,
                LocalDateTime.class,
                List.class
        );
        buildPrompt.setAccessible(true);

        String prompt = (String) buildPrompt.invoke(
                service,
                new AuthenticatedUser(1L, 2L, "employee.alice", "EMPLOYEE"),
                LocalDateTime.parse("2026-07-01T09:00:00"),
                LocalDateTime.parse("2026-07-08T09:00:00"),
                List.of(new UsageRecordResponse(
                        1L,
                        "Chrome",
                        LocalDateTime.parse("2026-07-02T10:00:00"),
                        LocalDateTime.parse("2026-07-02T11:00:00"),
                        LocalDateTime.parse("2026-07-02T11:01:00")
                ))
        );

        assertThat(prompt).contains("Write the report in Chinese");
        assertThat(prompt).contains("plain text only");
        assertThat(prompt).contains("Do not use Markdown");
    }

    @Test
    void teamReportPromptRequiresChinesePlainTextWithoutMarkdown() throws Exception {
        TeamReportServiceImpl service = new TeamReportServiceImpl(null, null, null);
        Method buildPrompt = TeamReportServiceImpl.class.getDeclaredMethod("buildPrompt", TeamUsageSummaryResponse.class);
        buildPrompt.setAccessible(true);

        String prompt = (String) buildPrompt.invoke(
                service,
                new TeamUsageSummaryResponse(
                        BigDecimal.valueOf(67.5),
                        135,
                        2,
                        List.of(new AppUsageRatioResponse("Slack", 75, BigDecimal.valueOf(0.56)))
                )
        );

        assertThat(prompt).contains("Write the report in Chinese");
        assertThat(prompt).contains("plain text only");
        assertThat(prompt).contains("Do not use Markdown");
    }
}
