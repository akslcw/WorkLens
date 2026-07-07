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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class WorklensBackendApplicationTests extends PostgresIntegrationTestSupport {

    private static final String PASSWORD = "Password123!";
    private static final String PASSWORD_HASH = "pbkdf2_sha256$120000$d29ya2xlbnMtc2FsdC0wMQ==$y7dDc5YjVRKR+v1GlPwEumSMa6Wa4bMH0h23Tk8Tx64=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        try {
            jdbcTemplate.execute("TRUNCATE TABLE employees RESTART IDENTITY CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE auth_tokens RESTART IDENTITY CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE auth_users RESTART IDENTITY CASCADE");
        } catch (Exception ignored) {
        }
    }

    @Test
    void healthEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpointReturnsOkForAuthenticatedUser() throws Exception {
        insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");
        String token = loginAndReadToken("manager", PASSWORD);

        mockMvc.perform(get("/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void loginReturnsTokenAndCurrentUserForManager() throws Exception {
        long employeeId = insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");

        MvcResult loginResult = login("manager", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.username").value("manager"))
                .andExpect(jsonPath("$.displayName").value("Manager User"))
                .andReturn();

        String token = readToken(loginResult);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.username").value("manager"))
                .andExpect(jsonPath("$.displayName").value("Manager User"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    void authMeRecognizesEmployeeRole() throws Exception {
        long employeeId = insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E001", "Alice");

        MvcResult loginResult = login("employee.alice", PASSWORD)
                .andExpect(status().isOk())
                .andReturn();

        String token = readToken(loginResult);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.username").value("employee.alice"))
                .andExpect(jsonPath("$.displayName").value("Alice"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"username\": \"manager\",
                                  \"password\": \"wrong-password\"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authMeRequiresToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsUserWithoutEmployeeBinding() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO auth_users (username, password_hash, role, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                "orphan.user",
                PASSWORD_HASH,
                "EMPLOYEE"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "orphan.user",
                                  "password": "Password123!"
                                }
                                """))
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

    private String readToken(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("token").asText();
    }
}
