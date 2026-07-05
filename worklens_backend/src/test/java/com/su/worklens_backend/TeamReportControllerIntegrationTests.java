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
class TeamReportControllerIntegrationTests extends PostgresIntegrationTestSupport {

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
    void generateTeamReportRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/llm/team-report"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCannotGenerateTeamReport() throws Exception {
        insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E001", "Alice");
        String employeeToken = loginAndReadToken("employee.alice", PASSWORD);

        mockMvc.perform(post("/llm/team-report")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanGenerateTeamReportUsingOnlyAggregatedSummary() throws Exception {
        insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");
        long aliceEmployeeId = insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E001", "Alice");
        long bobEmployeeId = insertUser("employee.bob", PASSWORD_HASH, "EMPLOYEE", "E002", "Bob");
        insertUsageRecord(aliceEmployeeId, "Slack", "2026-07-03 09:00:00", "2026-07-03 09:30:00");
        insertUsageRecord(aliceEmployeeId, "Chrome", "2026-07-03 10:00:00", "2026-07-03 11:00:00");
        insertUsageRecord(bobEmployeeId, "Slack", "2026-07-03 11:00:00", "2026-07-03 11:45:00");

        String managerToken = loginAndReadToken("manager", PASSWORD);
        given(llmProvider.generateText(org.mockito.ArgumentMatchers.anyString()))
                .willReturn("Team usage stayed steady this period, with Slack taking the largest share.");

        mockMvc.perform(post("/llm/team-report")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Team usage stayed steady this period, with Slack taking the largest share."));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmProvider).generateText(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("teamAverageUsageMinutes: 67.5");
        assertThat(prompt).contains("totalUsageMinutes: 135");
        assertThat(prompt).contains("activeEmployeeCount: 2");
        assertThat(prompt).contains("Slack");
        assertThat(prompt).contains("Chrome");
        assertThat(prompt).doesNotContain("Alice");
        assertThat(prompt).doesNotContain("Bob");
        assertThat(prompt).doesNotContain("employeeId");
        assertThat(prompt).doesNotContain("authUserId");
        assertThat(prompt).doesNotContain("startedAt");
        assertThat(prompt).doesNotContain("endedAt");
        assertThat(prompt).doesNotContain("2026-07-03T09:00:00");
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

    private void insertUsageRecord(long employeeId, String appName, String startedAt, String endedAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO usage_records (employee_id, app_name, started_at, ended_at, created_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                employeeId,
                appName,
                LocalDateTime.parse(startedAt.replace(" ", "T")),
                LocalDateTime.parse(endedAt.replace(" ", "T"))
        );
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

    private void truncateIfExists(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
        }
    }
}
