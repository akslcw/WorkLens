package com.su.worklens_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class UsageRecordControllerIntegrationTests extends PostgresIntegrationTestSupport {

    private static final String PASSWORD = "Password123!";
    private static final String PASSWORD_HASH = "pbkdf2_sha256$120000$d29ya2xlbnMtc2FsdC0wMQ==$y7dDc5YjVRKR+v1GlPwEumSMa6Wa4bMH0h23Tk8Tx64=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        truncateIfExists("TRUNCATE TABLE usage_records RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE auth_tokens RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE auth_users RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE employees RESTART IDENTITY CASCADE");
    }

    @Test
    void createUsageRecordRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/usage-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUsageRecordPayload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managerCannotCreateUsageRecord() throws Exception {
        insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");
        String managerToken = loginAndReadToken("manager", PASSWORD);

        mockMvc.perform(post("/usage-records")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUsageRecordPayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCanCreateUsageRecordForAuthenticatedUser() throws Exception {
        long employeeId = insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E001", "Alice");
        String employeeToken = loginAndReadToken("employee.alice", PASSWORD);

        MvcResult result = mockMvc.perform(post("/usage-records")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUsageRecordPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.appName").value("Slack"))
                .andExpect(jsonPath("$.startedAt").value("2026-07-03T09:00:00"))
                .andExpect(jsonPath("$.endedAt").value("2026-07-03T09:30:00"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        long usageRecordId = readId(result);
        Map<String, Object> persisted = jdbcTemplate.queryForMap(
                "SELECT employee_id, app_name, started_at, ended_at FROM usage_records WHERE id = ?",
                usageRecordId
        );

        assertThat(((Number) persisted.get("employee_id")).longValue()).isEqualTo(employeeId);
        assertThat(persisted.get("app_name")).isEqualTo("Slack");
        assertThat(((Timestamp) persisted.get("started_at")).toLocalDateTime()).isEqualTo(LocalDateTime.parse("2026-07-03T09:00:00"));
        assertThat(((Timestamp) persisted.get("ended_at")).toLocalDateTime()).isEqualTo(LocalDateTime.parse("2026-07-03T09:30:00"));
    }

    @Test
    void createUsageRecordRejectsEndBeforeStart() throws Exception {
        insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E001", "Alice");
        String employeeToken = loginAndReadToken("employee.alice", PASSWORD);

        mockMvc.perform(post("/usage-records")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appName": "Slack",
                                  "startedAt": "2026-07-03T10:00:00",
                                  "endedAt": "2026-07-03T09:30:00"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listUsageRecordsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/usage-records"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managerCannotListUsageRecords() throws Exception {
        insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");
        String managerToken = loginAndReadToken("manager", PASSWORD);

        mockMvc.perform(get("/usage-records")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCanOnlyListOwnUsageRecordsEvenIfQueryParameterIsForged() throws Exception {
        long aliceEmployeeId = insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E001", "Alice");
        long bobEmployeeId = insertUser("employee.bob", PASSWORD_HASH, "EMPLOYEE", "E002", "Bob");
        insertUsageRecord(aliceEmployeeId, "Slack", "2026-07-03T09:00:00", "2026-07-03T09:30:00");
        insertUsageRecord(aliceEmployeeId, "Chrome", "2026-07-03T10:00:00", "2026-07-03T11:00:00");
        insertUsageRecord(bobEmployeeId, "Teams", "2026-07-03T12:00:00", "2026-07-03T12:45:00");
        String aliceToken = loginAndReadToken("employee.alice", PASSWORD);

        mockMvc.perform(get("/usage-records")
                        .queryParam("employeeId", String.valueOf(bobEmployeeId))
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].appName").value("Slack"))
                .andExpect(jsonPath("$[0].startedAt").value("2026-07-03T09:00:00"))
                .andExpect(jsonPath("$[0].endedAt").value("2026-07-03T09:30:00"))
                .andExpect(jsonPath("$[1].appName").value("Chrome"))
                .andExpect(jsonPath("$[1].startedAt").value("2026-07-03T10:00:00"))
                .andExpect(jsonPath("$[1].endedAt").value("2026-07-03T11:00:00"))
                .andExpect(jsonPath("$[0].authUserId").doesNotExist())
                .andExpect(jsonPath("$[0].employeeId").doesNotExist())
                .andExpect(jsonPath("$[0].username").doesNotExist());
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

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password)));
    }

    private String loginAndReadToken(String username, String password) throws Exception {
        return readToken(login(username, password).andReturn());
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("id").asLong();
    }

    private String readToken(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("token").asText();
    }

    private String validUsageRecordPayload() {
        return """
                {
                  "appName": "Slack",
                  "startedAt": "2026-07-03T09:00:00",
                  "endedAt": "2026-07-03T09:30:00"
                }
                """;
    }

    private void insertUsageRecord(long employeeId, String appName, String startedAt, String endedAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO usage_records (employee_id, app_name, started_at, ended_at, created_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                employeeId,
                appName,
                LocalDateTime.parse(startedAt),
                LocalDateTime.parse(endedAt)
        );
    }

    private void truncateIfExists(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
        }
    }
}
