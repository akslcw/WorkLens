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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class EmployeeControllerIntegrationTests extends PostgresIntegrationTestSupport {

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
        try {
            jdbcTemplate.execute("TRUNCATE TABLE usage_records RESTART IDENTITY CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE auth_tokens RESTART IDENTITY CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE auth_users RESTART IDENTITY CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE employees RESTART IDENTITY CASCADE");
        } catch (Exception ignored) {
        }
    }

    @Test
    void createEmployeeRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice",
                                  "employeeNo": "E001"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeRoleCannotCreateEmployee() throws Exception {
        insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E900", "Alice");
        String employeeToken = loginAndReadToken("employee.alice", PASSWORD);

        mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice",
                                  "employeeNo": "E001"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanCreateEmployee() throws Exception {
        String managerToken = insertManagerAndLogin();

        mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice",
                                  "employeeNo": "E001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.employeeNo").value("E001"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void employeeRoleCannotListEmployees() throws Exception {
        insertUser("employee.alice", PASSWORD_HASH, "EMPLOYEE", "E900", "Alice");
        String employeeToken = loginAndReadToken("employee.alice", PASSWORD);

        mockMvc.perform(get("/employees")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanListEmployees() throws Exception {
        String managerToken = insertManagerAndLogin();
        createEmployee(managerToken, "Alice", "E001");
        createEmployee(managerToken, "Bob", "E002");

        MvcResult result = mockMvc.perform(get("/employees")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("\"name\":\"Alice\"");
        assertThat(responseBody).contains("\"employeeNo\":\"E001\"");
        assertThat(responseBody).contains("\"name\":\"Bob\"");
        assertThat(responseBody).contains("\"employeeNo\":\"E002\"");
    }

    @Test
    void managerCanGetEmployeeById() throws Exception {
        String managerToken = insertManagerAndLogin();
        long employeeId = createEmployee(managerToken, "Alice", "E001");

        mockMvc.perform(get("/employees/{id}", employeeId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.employeeNo").value("E001"));
    }

    @Test
    void managerCanUpdateEmployee() throws Exception {
        String managerToken = insertManagerAndLogin();
        long employeeId = createEmployee(managerToken, "Alice", "E001");

        mockMvc.perform(put("/employees/{id}", employeeId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice Zhang",
                                  "employeeNo": "E009"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId))
                .andExpect(jsonPath("$.name").value("Alice Zhang"))
                .andExpect(jsonPath("$.employeeNo").value("E009"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void managerCanDeleteEmployee() throws Exception {
        String managerToken = insertManagerAndLogin();
        long employeeId = createEmployee(managerToken, "Alice", "E001");

        mockMvc.perform(delete("/employees/{id}", employeeId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/employees/{id}", employeeId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM employees WHERE id = ?", Integer.class, employeeId);
        assertThat(count).isZero();
    }

    private void insertUser(String username, String passwordHash, String role, String employeeNo, String name) {
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
    }

    private String insertManagerAndLogin() throws Exception {
        insertUser("manager", PASSWORD_HASH, "MANAGER", "M001", "Manager User");
        return loginAndReadToken("manager", PASSWORD);
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

    private long createEmployee(String token, String name, String employeeNo) throws Exception {
        MvcResult result = mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "employeeNo": "%s"
                                }
                                """.formatted(name, employeeNo)))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("id").asLong();
    }

    private String readToken(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("token").asText();
    }
}
