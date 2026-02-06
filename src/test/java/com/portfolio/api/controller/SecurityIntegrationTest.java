package com.portfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.api.AbstractIntegrationTest;
import com.portfolio.api.dto.request.LoginRequest;
import com.portfolio.api.dto.request.TaskRequest;
import com.portfolio.api.dto.request.TaskStatusUpdateRequest;
import com.portfolio.api.dto.response.LoginResponse;
import com.portfolio.api.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken("admin", "admin123");
        userToken = obtainToken("user", "user123");
    }

    private String obtainToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponse.class
        );
        return response.token();
    }

    private RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    @Nested
    @DisplayName("Unauthenticated requests")
    class Unauthenticated {

        @Test
        @DisplayName("should return 401 with proper error shape when accessing protected endpoint without token")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get("/api/v1/tasks")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").value("/api/v1/tasks"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("should return 401 when token is invalid")
        void shouldReturn401WithInvalidToken() throws Exception {
            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", "Bearer invalid.token.here")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 401 when token is malformed")
        void shouldReturn401WithMalformedToken() throws Exception {
            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", "Bearer ")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should allow access to public auth endpoints")
        void shouldAllowPublicAuthEndpoints() throws Exception {
            LoginRequest loginRequest = new LoginRequest("admin", "admin123");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.expiresInMinutes").exists());
        }

        @Test
        @DisplayName("should allow access to actuator health")
        void shouldAllowActuatorHealth() throws Exception {
            mockMvc.perform(get("/actuator/health")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Authenticated USER requests")
    class AuthenticatedUser {

        @Test
        @DisplayName("should return 200 when USER accesses GET /tasks")
        void shouldAllowUserToGetTasks() throws Exception {
            mockMvc.perform(get("/api/v1/tasks")
                            .with(bearer(userToken))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 403 with proper error shape when USER tries to POST /tasks")
        void shouldDenyUserPostTask() throws Exception {
            TaskRequest request = new TaskRequest("Test Task", "Description", TaskStatus.TODO);

            mockMvc.perform(post("/api/v1/tasks")
                            .with(bearer(userToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").value("/api/v1/tasks"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("should return 403 when USER tries to DELETE /tasks/{id}")
        void shouldDenyUserDeleteTask() throws Exception {
            UUID taskId = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                            .with(bearer(userToken))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when USER tries to PATCH /tasks/{id}/status")
        void shouldDenyUserPatchStatus() throws Exception {
            UUID taskId = UUID.randomUUID();
            TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.DONE);

            mockMvc.perform(patch("/api/v1/tasks/{id}/status", taskId)
                            .with(bearer(userToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Authenticated ADMIN requests")
    class AuthenticatedAdmin {

        @Test
        @DisplayName("should return 200 when ADMIN accesses GET /tasks")
        void shouldAllowAdminToGetTasks() throws Exception {
            mockMvc.perform(get("/api/v1/tasks")
                            .with(bearer(adminToken))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 201 when ADMIN creates task")
        void shouldAllowAdminToCreateTask() throws Exception {
            TaskRequest request = new TaskRequest("Admin Task", "Description", TaskStatus.TODO);

            mockMvc.perform(post("/api/v1/tasks")
                            .with(bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Admin Task"))
                    .andExpect(jsonPath("$.status").value("TODO"));
        }

        @Test
        @DisplayName("should return 200 when ADMIN updates task status")
        void shouldAllowAdminToPatchStatus() throws Exception {
            TaskRequest createRequest = new TaskRequest("Task to Update", "Description", TaskStatus.TODO);

            MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
                            .with(bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("id").asText();

            TaskStatusUpdateRequest statusRequest = new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS);

            mockMvc.perform(patch("/api/v1/tasks/{id}/status", taskId)
                            .with(bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("should return 204 when ADMIN deletes task")
        void shouldAllowAdminToDeleteTask() throws Exception {
            TaskRequest createRequest = new TaskRequest("Task to Delete", "Description", TaskStatus.TODO);

            MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
                            .with(bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("id").asText();

            mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                            .with(bearer(adminToken))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }
    }
}
