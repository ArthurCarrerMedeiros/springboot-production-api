package com.portfolio.api.controller;

import com.portfolio.api.dto.request.TaskRequest;
import com.portfolio.api.dto.request.TaskStatusUpdateRequest;
import com.portfolio.api.dto.response.TaskResponse;
import com.portfolio.api.model.TaskStatus;
import com.portfolio.api.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "List all tasks", description = "Returns a paginated list of tasks with optional status filter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    })
    public ResponseEntity<Page<TaskResponse>> findAll(
            @Parameter(description = "Filter by task status")
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<TaskResponse> tasks = taskService.findAll(status, pageable);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Returns a single task by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task found"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> findById(
            @Parameter(description = "Task ID")
            @PathVariable UUID id) {

        TaskResponse task = taskService.findById(id);
        return ResponseEntity.ok(task);
    }

    @PostMapping
    @Operation(summary = "Create a new task", description = "Creates a new task and returns it")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Task created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody TaskRequest request) {

        TaskResponse task = taskService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task", description = "Updates an existing task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> update(
            @Parameter(description = "Task ID")
            @PathVariable UUID id,
            @Valid @RequestBody TaskRequest request) {

        TaskResponse task = taskService.update(id, request);
        return ResponseEntity.ok(task);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status", description = "Updates only the status of an existing task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> updateStatus(
            @Parameter(description = "Task ID")
            @PathVariable UUID id,
            @Valid @RequestBody TaskStatusUpdateRequest request) {

        TaskResponse task = taskService.updateStatus(id, request);
        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task", description = "Deletes an existing task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Task ID")
            @PathVariable UUID id) {

        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
