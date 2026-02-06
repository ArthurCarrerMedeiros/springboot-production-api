package com.portfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.api.dto.request.TaskRequest;
import com.portfolio.api.dto.request.TaskStatusUpdateRequest;
import com.portfolio.api.dto.response.TaskResponse;
import com.portfolio.api.model.TaskStatus;
import com.portfolio.api.service.TaskService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@DisplayName("TaskController")
class TaskControllerTest {

    private static final String BASE_URL = "/api/v1/tasks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    private UUID taskId;
    private TaskResponse taskResponse;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        now = LocalDateTime.of(2026, 1, 1, 12, 0);

        taskResponse = new TaskResponse(
                taskId,
                "Test Task",
                "Test Description",
                TaskStatus.TODO,
                now,
                now
        );
    }

    @Nested
    @DisplayName("GET /api/v1/tasks")
    class FindAll {

        @Test
        @DisplayName("should return 200 with paginated tasks")
        void shouldReturnPaginatedTasks() throws Exception {
            var page = new PageImpl<>(List.of(taskResponse));
            when(taskService.findAll(any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(taskId.toString())))
                    .andExpect(jsonPath("$.content[0].title", is("Test Task")))
                    .andExpect(jsonPath("$.content[0].status", is("TODO")));

            verify(taskService).findAll(any(), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by status when provided")
        void shouldFilterByStatus() throws Exception {
            var page = new PageImpl<>(List.of(taskResponse));
            when(taskService.findAll(eq(TaskStatus.TODO), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get(BASE_URL)
                            .param("status", "TODO")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));

            verify(taskService).findAll(eq(TaskStatus.TODO), any(Pageable.class));
        }

        @Test
        @DisplayName("should return empty page when no tasks")
        void shouldReturnEmptyPage() throws Exception {
            var emptyPage = new PageImpl<TaskResponse>(List.of());
            when(taskService.findAll(any(), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tasks/{id}")
    class FindById {

        @Test
        @DisplayName("should return 200 with task when found")
        void shouldReturnTaskWhenFound() throws Exception {
            when(taskService.findById(taskId)).thenReturn(taskResponse);

            mockMvc.perform(get(BASE_URL + "/{id}", taskId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(taskId.toString())))
                    .andExpect(jsonPath("$.title", is("Test Task")))
                    .andExpect(jsonPath("$.description", is("Test Description")))
                    .andExpect(jsonPath("$.status", is("TODO")));

            verify(taskService).findById(taskId);
        }

        @Test
        @DisplayName("should return 404 when task not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(taskService.findById(taskId))
                    .thenThrow(new EntityNotFoundException("Task not found with id: " + taskId));

            mockMvc.perform(get(BASE_URL + "/{id}", taskId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message").exists());

            verify(taskService).findById(taskId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tasks")
    class Create {

        @Test
        @DisplayName("should return 201 when task created successfully")
        void shouldReturn201WhenCreated() throws Exception {
            TaskRequest request = new TaskRequest("Test Task", "Test Description", TaskStatus.TODO);
            when(taskService.create(any(TaskRequest.class))).thenReturn(taskResponse);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(taskId.toString())))
                    .andExpect(jsonPath("$.title", is("Test Task")));

            verify(taskService).create(any(TaskRequest.class));
        }

        @Test
        @DisplayName("should return 400 when title is blank")
        void shouldReturn400WhenTitleBlank() throws Exception {
            TaskRequest request = new TaskRequest("", "Description", TaskStatus.TODO);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(taskService, never()).create(any());
        }

        @Test
        @DisplayName("should return 400 when status is null")
        void shouldReturn400WhenStatusNull() throws Exception {
            String invalidJson = """
                    {
                        "title": "Test Task",
                        "description": "Description"
                    }
                    """;

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(taskService, never()).create(any());
        }

        @Test
        @DisplayName("should return 400 when title exceeds max length")
        void shouldReturn400WhenTitleTooLong() throws Exception {
            String longTitle = "a".repeat(256);
            TaskRequest request = new TaskRequest(longTitle, "Description", TaskStatus.TODO);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(taskService, never()).create(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/tasks/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 when task updated successfully")
        void shouldReturn200WhenUpdated() throws Exception {
            TaskRequest request = new TaskRequest("Updated Title", "Updated Description", TaskStatus.IN_PROGRESS);
            TaskResponse updatedResponse = new TaskResponse(
                    taskId, "Updated Title", "Updated Description", TaskStatus.IN_PROGRESS, now, now
            );
            when(taskService.update(eq(taskId), any(TaskRequest.class))).thenReturn(updatedResponse);

            mockMvc.perform(put(BASE_URL + "/{id}", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("Updated Title")))
                    .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

            verify(taskService).update(eq(taskId), any(TaskRequest.class));
        }

        @Test
        @DisplayName("should return 404 when updating non-existent task")
        void shouldReturn404WhenUpdatingNonExistent() throws Exception {
            TaskRequest request = new TaskRequest("Title", "Description", TaskStatus.TODO);
            when(taskService.update(eq(taskId), any(TaskRequest.class)))
                    .thenThrow(new EntityNotFoundException("Task not found with id: " + taskId));

            mockMvc.perform(put(BASE_URL + "/{id}", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));

            verify(taskService).update(eq(taskId), any(TaskRequest.class));
        }

        @Test
        @DisplayName("should return 400 when request body is invalid")
        void shouldReturn400WhenInvalidBody() throws Exception {
            TaskRequest request = new TaskRequest("", null, null);

            mockMvc.perform(put(BASE_URL + "/{id}", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).update(any(), any());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/tasks/{id}/status")
    class UpdateStatus {

        @Test
        @DisplayName("should return 200 when status updated successfully")
        void shouldReturn200WhenStatusUpdated() throws Exception {
            TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.DONE);
            TaskResponse doneResponse = new TaskResponse(
                    taskId, "Test Task", "Test Description", TaskStatus.DONE, now, now
            );
            when(taskService.updateStatus(eq(taskId), any(TaskStatusUpdateRequest.class)))
                    .thenReturn(doneResponse);

            mockMvc.perform(patch(BASE_URL + "/{id}/status", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("DONE")));

            verify(taskService).updateStatus(eq(taskId), any(TaskStatusUpdateRequest.class));
        }

        @Test
        @DisplayName("should return 404 when task not found")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.DONE);
            when(taskService.updateStatus(eq(taskId), any(TaskStatusUpdateRequest.class)))
                    .thenThrow(new EntityNotFoundException("Task not found"));

            mockMvc.perform(patch(BASE_URL + "/{id}/status", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            verify(taskService).updateStatus(eq(taskId), any(TaskStatusUpdateRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/tasks/{id}")
    class Delete {

        @Test
        @DisplayName("should return 204 when task deleted successfully")
        void shouldReturn204WhenDeleted() throws Exception {
            doNothing().when(taskService).delete(taskId);

            mockMvc.perform(delete(BASE_URL + "/{id}", taskId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(taskService).delete(taskId);
        }

        @Test
        @DisplayName("should return 404 when deleting non-existent task")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            doThrow(new EntityNotFoundException("Task not found with id: " + taskId))
                    .when(taskService).delete(taskId);

            mockMvc.perform(delete(BASE_URL + "/{id}", taskId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));

            verify(taskService).delete(taskId);
        }
    }
}
