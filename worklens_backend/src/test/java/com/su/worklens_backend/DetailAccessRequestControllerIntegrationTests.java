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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class DetailAccessRequestControllerIntegrationTests extends PostgresIntegrationTestSupport {

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
        truncateIfExists("TRUNCATE TABLE detail_access_requests RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE auth_tokens RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE auth_users RESTART IDENTITY CASCADE");
        truncateIfExists("TRUNCATE TABLE employees RESTART IDENTITY CASCADE");
    }

    @Test
    void createDetailAccessRequestRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/detail-access-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestPayload(2L)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCannotCreateDetailAccessRequest() throws Exception {
        insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E001", "Alice");
        insertEmployee("E002", "Bob");
        String employeeToken = loginAndReadToken("employee.alice", PASSWORD);

        mockMvc.perform(post("/detail-access-requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestPayload(2L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanCreatePendingDetailAccessRequest() throws Exception {
        long managerEmployeeId = insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");
        long targetEmployeeId = insertEmployee("E002", "Bob");
        String managerToken = loginAndReadToken("manager", PASSWORD);

        MvcResult result = mockMvc.perform(post("/detail-access-requests")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestPayload(targetEmployeeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.requesterEmployeeId").value(managerEmployeeId))
                .andExpect(jsonPath("$.targetEmployeeId").value(targetEmployeeId))
                .andExpect(jsonPath("$.reason").value("Quarterly compliance review"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.processedAt").doesNotExist())
                .andExpect(jsonPath("$.processedByEmployeeId").doesNotExist())
                .andReturn();

        long requestId = readId(result);
        Map<String, Object> persisted = jdbcTemplate.queryForMap(
                """
                        SELECT requester_employee_id, target_employee_id, reason, status, created_at, processed_at, processed_by_employee_id
                        FROM detail_access_requests
                        WHERE id = ?
                        """,
                requestId
        );

        assertThat(((Number) persisted.get("requester_employee_id")).longValue()).isEqualTo(managerEmployeeId);
        assertThat(((Number) persisted.get("target_employee_id")).longValue()).isEqualTo(targetEmployeeId);
        assertThat(persisted.get("reason")).isEqualTo("Quarterly compliance review");
        assertThat(persisted.get("status")).isEqualTo("PENDING");
        assertThat(((Timestamp) persisted.get("created_at")).toLocalDateTime()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(persisted.get("processed_at")).isNull();
        assertThat(persisted.get("processed_by_employee_id")).isNull();
    }

    @Test
    void decisionRequiresAuthentication() throws Exception {
        long requesterEmployeeId = insertEmployee("M001", "Manager User");
        long targetEmployeeId = insertEmployee("E001", "Alice");
        long requestId = insertDetailAccessRequest(requesterEmployeeId, targetEmployeeId, "Quarterly compliance review", "PENDING");

        mockMvc.perform(patch("/detail-access-requests/{id}/decision", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDecisionPayload("APPROVED")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managerCannotApproveDetailAccessRequest() throws Exception {
        long managerEmployeeId = insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");
        long targetEmployeeId = insertUser("employee.bob", PASSWORD_HASH, "EMPLOYEE", "E002", "Bob");
        long requestId = insertDetailAccessRequest(managerEmployeeId, targetEmployeeId, "Quarterly compliance review", "PENDING");
        String managerToken = loginAndReadToken("manager", PASSWORD);

        mockMvc.perform(patch("/detail-access-requests/{id}/decision", requestId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDecisionPayload("APPROVED")))
                .andExpect(status().isForbidden());
    }

    @Test
    void otherEmployeeCannotApproveDetailAccessRequest() throws Exception {
        long managerEmployeeId = insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");
        long targetEmployeeId = insertUser("employee.bob", PASSWORD_HASH, "EMPLOYEE", "E002", "Bob");
        insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E003", "Alice");
        long requestId = insertDetailAccessRequest(managerEmployeeId, targetEmployeeId, "Quarterly compliance review", "PENDING");
        String aliceToken = loginAndReadToken("employee.alice", PASSWORD);

        mockMvc.perform(patch("/detail-access-requests/{id}/decision", requestId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDecisionPayload("APPROVED")))
                .andExpect(status().isForbidden());
    }

    @Test
    void targetEmployeeCanApproveOwnDetailAccessRequest() throws Exception {
        long managerEmployeeId = insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");
        long targetEmployeeId = insertUser("employee.bob", PASSWORD_HASH, "EMPLOYEE", "E002", "Bob");
        long requestId = insertDetailAccessRequest(managerEmployeeId, targetEmployeeId, "Quarterly compliance review", "PENDING");
        String bobToken = loginAndReadToken("employee.bob", PASSWORD);

        mockMvc.perform(patch("/detail-access-requests/{id}/decision", requestId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDecisionPayload("APPROVED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.processedAt").isNotEmpty())
                .andExpect(jsonPath("$.processedByEmployeeId").value(targetEmployeeId));

        Map<String, Object> persisted = jdbcTemplate.queryForMap(
                "SELECT status, processed_at, processed_by_employee_id FROM detail_access_requests WHERE id = ?",
                requestId
        );

        assertThat(persisted.get("status")).isEqualTo("APPROVED");
        assertThat(persisted.get("processed_at")).isNotNull();
        assertThat(((Number) persisted.get("processed_by_employee_id")).longValue()).isEqualTo(targetEmployeeId);
    }

    @Test
    void targetEmployeeCanRejectOwnDetailAccessRequest() throws Exception {
        long managerEmployeeId = insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");
        long targetEmployeeId = insertUser("employee.bob", PASSWORD_HASH, "EMPLOYEE", "E002", "Bob");
        long requestId = insertDetailAccessRequest(managerEmployeeId, targetEmployeeId, "Quarterly compliance review", "PENDING");
        String bobToken = loginAndReadToken("employee.bob", PASSWORD);

        mockMvc.perform(patch("/detail-access-requests/{id}/decision", requestId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDecisionPayload("REJECTED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.processedAt").isNotEmpty())
                .andExpect(jsonPath("$.processedByEmployeeId").value(targetEmployeeId));
    }

    private long insertUser(String username, String passwordHash, String role, String employeeNo, String name) {
        long employeeId = insertEmployee(employeeNo, name);
        jdbcTemplate.update(
                "INSERT INTO auth_users (username, password_hash, role, employee_id, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                username,
                passwordHash,
                role,
                employeeId
        );
        return employeeId;
    }

    private long insertEmployee(String employeeNo, String name) {
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
        assertThat(employeeId).isNotNull();
        return employeeId;
    }

    private long insertDetailAccessRequest(long requesterEmployeeId, long targetEmployeeId, String reason, String status) {
        jdbcTemplate.update(
                """
                        INSERT INTO detail_access_requests (
                            requester_employee_id,
                            target_employee_id,
                            reason,
                            status,
                            created_at,
                            processed_at,
                            processed_by_employee_id
                        ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, NULL, NULL)
                        """,
                requesterEmployeeId,
                targetEmployeeId,
                reason,
                status
        );
        Long requestId = jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM detail_access_requests
                        WHERE requester_employee_id = ? AND target_employee_id = ? AND reason = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                Long.class,
                requesterEmployeeId,
                targetEmployeeId,
                reason
        );
        assertThat(requestId).isNotNull();
        return requestId;
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

    private String validRequestPayload(long targetEmployeeId) {
        return """
                {
                  "targetEmployeeId": %d,
                  "reason": "Quarterly compliance review"
                }
                """.formatted(targetEmployeeId);
    }

    private String validDecisionPayload(String decision) {
        return """
                {
                  "decision": "%s"
                }
                """.formatted(decision);
    }

    private void truncateIfExists(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
        }
    }
}
