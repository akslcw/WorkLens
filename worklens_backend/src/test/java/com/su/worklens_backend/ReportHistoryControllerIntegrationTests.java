package com.su.worklens_backend;

import com.su.worklens_backend.service.LlmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class ReportHistoryControllerIntegrationTests extends PostgresIntegrationTestSupport {

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
        truncateIfExists("TRUNCATE TABLE llm_reports RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE detail_access_audit_logs RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE detail_access_requests RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE usage_records RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE auth_tokens RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE auth_users RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE employees RESTART IDENTITY CASCADE");
    }

    @Test
    void employeeReportHistoryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/llm/employee-report-history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCanListOwnArchivedReportHistory() throws Exception {
        long aliceEmployeeId = insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E001", "Alice");
        long bobEmployeeId = insertUser("employee.bob", PASSWORD_HASH, "EMPLOYEE", "E002", "Bob");
        String aliceToken = loginAndReadToken("employee.alice", PASSWORD);
        insertReport("EMPLOYEE_WEEKLY", aliceEmployeeId, aliceEmployeeId, "First employee report",
                LocalDateTime.parse("2026-06-29T00:00:00"), LocalDateTime.parse("2026-07-05T23:59:59"),
                LocalDateTime.parse("2026-07-06T00:10:00"));
        insertReport("EMPLOYEE_WEEKLY", aliceEmployeeId, aliceEmployeeId, "Second employee report",
                LocalDateTime.parse("2026-07-06T00:00:00"), LocalDateTime.parse("2026-07-12T23:59:59"),
                LocalDateTime.parse("2026-07-13T00:10:00"));
        insertReport("EMPLOYEE_WEEKLY", bobEmployeeId, bobEmployeeId, "Bob employee report",
                LocalDateTime.parse("2026-07-06T00:00:00"), LocalDateTime.parse("2026-07-12T23:59:59"),
                LocalDateTime.parse("2026-07-13T00:15:00"));

        mockMvc.perform(get("/llm/employee-report-history")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].summary").value("Second employee report"))
                .andExpect(jsonPath("$[1].summary").value("First employee report"))
                .andExpect(jsonPath("$[0].reportType").value("EMPLOYEE_WEEKLY"))
                .andExpect(jsonPath("$[0].periodStartedAt").isNotEmpty())
                .andExpect(jsonPath("$[0].periodEndedAt").isNotEmpty())
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$[0].requesterEmployeeId").doesNotExist())
                .andExpect(jsonPath("$[0].targetEmployeeId").doesNotExist());
    }

    @Test
    void managerCannotReadEmployeeReportHistory() throws Exception {
        insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");
        String managerToken = loginAndReadToken("manager", PASSWORD);

        mockMvc.perform(get("/llm/employee-report-history")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanListOwnArchivedTeamReportHistory() throws Exception {
        long managerOneId = insertUser("manager.one", PASSWORD_HASH, "MANAGER", "M001", "Manager One");
        long managerTwoId = insertUser("manager.two", PASSWORD_HASH, "MANAGER", "M002", "Manager Two");
        String managerOneToken = loginAndReadToken("manager.one", PASSWORD);
        insertReport("TEAM_SUMMARY", managerOneId, null, "First team report",
                null, null, LocalDateTime.parse("2026-07-06T00:10:00"));
        insertReport("TEAM_SUMMARY", managerOneId, null, "Second team report",
                null, null, LocalDateTime.parse("2026-07-13T00:10:00"));
        insertReport("TEAM_SUMMARY", managerTwoId, null, "Other manager team report",
                null, null, LocalDateTime.parse("2026-07-13T00:15:00"));

        mockMvc.perform(get("/llm/team-report-history")
                        .header("Authorization", "Bearer " + managerOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].summary").value("Second team report"))
                .andExpect(jsonPath("$[1].summary").value("First team report"))
                .andExpect(jsonPath("$[0].reportType").value("TEAM_SUMMARY"))
                .andExpect(jsonPath("$[0].periodStartedAt").isEmpty())
                .andExpect(jsonPath("$[0].periodEndedAt").isEmpty())
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$[0].requesterEmployeeId").doesNotExist())
                .andExpect(jsonPath("$[0].targetEmployeeId").doesNotExist());
    }

    @Test
    void employeeCannotReadTeamReportHistory() throws Exception {
        insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E001", "Alice");
        String employeeToken = loginAndReadToken("employee.alice", PASSWORD);

        mockMvc.perform(get("/llm/team-report-history")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
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

    private void insertReport(
            String reportType,
            Long requesterEmployeeId,
            Long targetEmployeeId,
            String summary,
            LocalDateTime periodStartedAt,
            LocalDateTime periodEndedAt,
            LocalDateTime createdAt
    ) {
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
                        VALUES (?, ?, ?, ?, ?, ?, ?, null, null, null, null, '[]'::jsonb, null, null, null)
                        """,
                reportType,
                requesterEmployeeId,
                targetEmployeeId,
                summary,
                periodStartedAt,
                periodEndedAt,
                createdAt
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
