package com.su.worklens_backend;

import com.su.worklens_backend.service.LlmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class EmployeeReportControllerIntegrationTests extends PostgresIntegrationTestSupport {

    private static final String PASSWORD = "Password123!";
    private static final String PASSWORD_HASH = "pbkdf2_sha256$120000$d29ya2xlbnMtc2FsdC0wMQ==$y7dDc5YjVRKR+v1GlPwEumSMa6Wa4bMH0h23Tk8Tx64=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private LlmProvider llmProvider;

    @BeforeEach
    void cleanDatabase() {
        truncateIfExists("TRUNCATE TABLE detail_access_audit_logs RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE detail_access_requests RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE usage_records RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE auth_tokens RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE auth_users RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE employees RESTART IDENTITY CASCADE");
    }

    @Test
    void generateEmployeeReportRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/llm/employee-report"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managerCannotGenerateEmployeeReport() throws Exception {
        insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");
        String managerToken = loginAndReadToken("manager", PASSWORD);

        mockMvc.perform(post("/llm/employee-report")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCanGenerateEncouragingWeeklyReportUsingOnlyOwnRecentRecords() throws Exception {
        long aliceEmployeeId = insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E001", "Alice");
        long bobEmployeeId = insertUser("employee.bob", PASSWORD_HASH, "EMPLOYEE", "E002", "Bob");
        LocalDateTime now = LocalDateTime.now();
        insertUsageRecord(aliceEmployeeId, "Slack", now.minusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0), now.minusDays(2).withHour(9).withMinute(30).withSecond(0).withNano(0));
        insertUsageRecord(aliceEmployeeId, "Chrome", now.minusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0), now.minusDays(1).withHour(11).withMinute(15).withSecond(0).withNano(0));
        insertUsageRecord(aliceEmployeeId, "LegacyApp", now.minusDays(8).withHour(8).withMinute(0).withSecond(0).withNano(0), now.minusDays(8).withHour(8).withMinute(45).withSecond(0).withNano(0));
        insertUsageRecord(bobEmployeeId, "Teams", now.minusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0), now.minusDays(1).withHour(14).withMinute(30).withSecond(0).withNano(0));

        String aliceToken = loginAndReadToken("employee.alice", PASSWORD);
        given(llmProvider.generateText(org.mockito.ArgumentMatchers.anyString()))
                .willReturn("You kept a solid rhythm this week and built long focus blocks in Chrome.");

        mockMvc.perform(post("/llm/employee-report")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("You kept a solid rhythm this week and built long focus blocks in Chrome."));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmProvider).generateText(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("encouraging");
        assertThat(prompt).contains("Slack");
        assertThat(prompt).contains("Chrome");
        assertThat(prompt).doesNotContain("LegacyApp");
        assertThat(prompt).doesNotContain("Teams");
    }

    private long insertUser(String username, String passwordHash, String role, String employeeNo, String name) {
        jdbcTemplate.update(
                "INSERT INTO employees (name, employee_no, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                name,
                employeeNo
        );
        Long employeeId = jdbcTemplate.queryForObject(
                "SELECT id FROM employees WHERE employee_no = ?",
                Long.class,
                employeeNo
        );
        jdbcTemplate.update(
                "INSERT INTO auth_users (username, password_hash, role, employee_id, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                username,
                passwordHash,
                role,
                employeeId
        );
        assertThat(employeeId).isNotNull();
        return employeeId;
    }

    private String loginAndReadToken(String username, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int tokenStart = responseBody.indexOf("\"token\":\"");
        int valueStart = tokenStart + 9;
        int valueEnd = responseBody.indexOf("\"", valueStart);
        return responseBody.substring(valueStart, valueEnd);
    }

    private void insertUsageRecord(long employeeId, String appName, LocalDateTime startedAt, LocalDateTime endedAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO usage_records (employee_id, app_name, started_at, ended_at, created_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                employeeId,
                appName,
                startedAt,
                endedAt
        );
    }

    private void truncateIfExists(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
        }
    }
}
