package com.portfolio.api.service;

import com.portfolio.api.dto.request.TaskRequest;
import com.portfolio.api.dto.request.TaskStatusUpdateRequest;
import com.portfolio.api.dto.response.TaskResponse;
import com.portfolio.api.mapper.TaskMapper;
import com.portfolio.api.model.Task;
import com.portfolio.api.model.TaskStatus;
import com.portfolio.api.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Transactional(readOnly = true)
    public Page<TaskResponse> findAll(TaskStatus status, Pageable pageable) {
        Page<Task> tasks = (status != null)
                ? taskRepository.findByStatus(status, pageable)
                : taskRepository.findAll(pageable);

        return tasks.map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(UUID id) {
        Task task = findTaskById(id);
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = taskMapper.toEntity(request);
        Task savedTask = taskRepository.save(task);
        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    public TaskResponse update(UUID id, TaskRequest request) {
        Task task = findTaskById(id);
        taskMapper.updateEntity(task, request);
        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    @Transactional
    public TaskResponse updateStatus(UUID id, TaskStatusUpdateRequest request) {
        Task task = findTaskById(id);
        task.setStatus(request.status());
        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    @Transactional
    public void delete(UUID id) {
        Task task = findTaskById(id);
        taskRepository.delete(task);
    }

    private Task findTaskById(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));
    }
}
