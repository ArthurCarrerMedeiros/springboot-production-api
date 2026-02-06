package com.portfolio.api.service;

import com.portfolio.api.dto.request.TaskRequest;
import com.portfolio.api.dto.request.TaskStatusUpdateRequest;
import com.portfolio.api.dto.response.TaskResponse;
import com.portfolio.api.mapper.TaskMapper;
import com.portfolio.api.model.Task;
import com.portfolio.api.model.TaskStatus;
import com.portfolio.api.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private UUID taskId;
    private Task task;
    private TaskRequest taskRequest;
    private TaskResponse taskResponse;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        now = LocalDateTime.of(2026, 1, 1, 12, 0);

        task = Task.builder()
                .id(taskId)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .createdAt(now)
                .updatedAt(now)
                .build();

        taskRequest = new TaskRequest("Test Task", "Test Description", TaskStatus.TODO);

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
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create task and verify full flow: toEntity -> save -> toResponse")
        void shouldCreateTask() {
            when(taskMapper.toEntity(taskRequest)).thenReturn(task);
            when(taskRepository.save(task)).thenReturn(task);
            when(taskMapper.toResponse(task)).thenReturn(taskResponse);

            TaskResponse result = taskService.create(taskRequest);

            assertThat(result).isEqualTo(taskResponse);
            verify(taskMapper).toEntity(taskRequest);
            verify(taskRepository).save(task);
            verify(taskMapper).toResponse(task);
            verifyNoMoreInteractions(taskRepository, taskMapper);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return task when found")
        void shouldReturnTaskWhenFound() {
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(taskResponse);

            TaskResponse result = taskService.findById(taskId);

            assertThat(result).isEqualTo(taskResponse);
            verify(taskRepository).findById(taskId);
            verify(taskMapper).toResponse(task);
            verifyNoMoreInteractions(taskRepository, taskMapper);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException and never map when task not found")
        void shouldThrowWhenNotFound() {
            when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.findById(taskId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(taskId.toString());

            verify(taskRepository).findById(taskId);
            verify(taskMapper, never()).toResponse(any());
            verifyNoMoreInteractions(taskRepository, taskMapper);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return page with multiple tasks and verify mapper called for each")
        void shouldReturnPageOfTasks() {
            UUID taskId2 = UUID.randomUUID();

            Task task2 = Task.builder()
                    .id(taskId2)
                    .title("Task 2")
                    .description("Description 2")
                    .status(TaskStatus.IN_PROGRESS)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            TaskResponse taskResponse2 = new TaskResponse(
                    taskId2, "Task 2", "Description 2", TaskStatus.IN_PROGRESS, now, now
            );

            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(task, task2), pageable, 2);

            when(taskRepository.findAll(pageable)).thenReturn(taskPage);
            when(taskMapper.toResponse(task)).thenReturn(taskResponse);
            when(taskMapper.toResponse(task2)).thenReturn(taskResponse2);

            Page<TaskResponse> result = taskService.findAll(null, pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).containsExactly(taskResponse, taskResponse2);
            verify(taskRepository).findAll(pageable);
            verify(taskMapper, times(2)).toResponse(any(Task.class));
            verifyNoMoreInteractions(taskRepository, taskMapper);
        }

        @Test
        @DisplayName("should filter by status when provided")
        void shouldFilterByStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(task), pageable, 1);

            when(taskRepository.findByStatus(TaskStatus.TODO, pageable)).thenReturn(taskPage);
            when(taskMapper.toResponse(task)).thenReturn(taskResponse);

            Page<TaskResponse> result = taskService.findAll(TaskStatus.TODO, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(taskRepository).findByStatus(TaskStatus.TODO, pageable);
            verify(taskRepository, never()).findAll(any(Pageable.class));
            verify(taskMapper, times(1)).toResponse(task);
            verifyNoMoreInteractions(taskRepository, taskMapper);
        }

        @Test
        @DisplayName("should return empty page when no tasks exist")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(taskRepository.findAll(pageable)).thenReturn(emptyPage);

            Page<TaskResponse> result = taskService.findAll(null, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(taskRepository).findAll(pageable);
            verify(taskMapper, never()).toResponse(any());
            verifyNoMoreInteractions(taskRepository, taskMapper);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update task and verify entity was altered before save")
        void shouldUpdateTask() {
            TaskRequest updateRequest = new TaskRequest(
                    "Updated Title",
                    "Updated Description",
                    TaskStatus.IN_PROGRESS
            );
            TaskResponse updatedResponse = new TaskResponse(
                    taskId,
                    "Updated Title",
                    "Updated Description",
                    TaskStatus.IN_PROGRESS,
                    now,
                    now
            );

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            doAnswer(inv -> {
                Task entity = inv.getArgument(0);
                TaskRequest req = inv.getArgument(1);
                entity.setTitle(req.title());
                entity.setDescription(req.description());
                entity.setStatus(req.status());
                return null;
            }).when(taskMapper).updateEntity(any(Task.class), any(TaskRequest.class));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
            when(taskMapper.toResponse(any(Task.class))).thenReturn(updatedResponse);

            TaskResponse result = taskService.update(taskId, updateRequest);

            assertThat(result.title()).isEqualTo("Updated Title");
            assertThat(result.status()).isEqualTo(TaskStatus.IN_PROGRESS);

            verify(taskRepository).findById(taskId);
            verify(taskMapper).updateEntity(task, updateRequest);
            verify(taskRepository).save(argThat(saved ->
                    "Updated Title".equals(saved.getTitle()) &&
                    "Updated Description".equals(saved.getDescription()) &&
                    saved.getStatus() == TaskStatus.IN_PROGRESS
            ));
            verify(taskMapper).toResponse(task);
            verifyNoMoreInteractions(taskRepository, taskMapper);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException and never save when task not found")
        void shouldThrowWhenUpdatingNonExistent() {
            TaskRequest updateRequest = new TaskRequest(
                    "Updated Title",
                    "Updated Description",
                    TaskStatus.IN_PROGRESS
            );

            when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.update(taskId, updateRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(taskId.toString());

            verify(taskRepository).findById(taskId);
            verify(taskRepository, never()).save(any());
            verify(taskMapper, never()).updateEntity(any(), any());
            verify(taskMapper, never()).toResponse(any());
            verifyNoMoreInteractions(taskRepository, taskMapper);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("should update only status and verify other fields unchanged")
        void shouldUpdateStatus() {
            TaskStatusUpdateRequest statusRequest = new TaskStatusUpdateRequest(TaskStatus.DONE);
            TaskResponse doneResponse = new TaskResponse(
                    taskId,
                    "Test Task",
                    "Test Description",
                    TaskStatus.DONE,
                    now,
                    now
            );

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
            when(taskMapper.toResponse(any(Task.class))).thenReturn(doneResponse);

            TaskResponse result = taskService.updateStatus(taskId, statusRequest);

            assertThat(result.status()).isEqualTo(TaskStatus.DONE);
            verify(taskRepository).findById(taskId);
            verify(taskRepository).save(argThat(saved ->
                    saved.getStatus() == TaskStatus.DONE &&
                    "Test Task".equals(saved.getTitle()) &&
                    "Test Description".equals(saved.getDescription())
            ));
            verify(taskMapper).toResponse(task);
            verifyNoMoreInteractions(taskRepository, taskMapper);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when task not found")
        void shouldThrowWhenStatusUpdateOnNonExistent() {
            TaskStatusUpdateRequest statusRequest = new TaskStatusUpdateRequest(TaskStatus.DONE);

            when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateStatus(taskId, statusRequest))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(taskRepository).findById(taskId);
            verify(taskRepository, never()).save(any());
            verifyNoMoreInteractions(taskRepository, taskMapper);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete task when found")
        void shouldDeleteTask() {
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

            taskService.delete(taskId);

            verify(taskRepository).findById(taskId);
            verify(taskRepository).delete(task);
            verifyNoMoreInteractions(taskRepository, taskMapper);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException and never delete when task not found")
        void shouldThrowWhenDeletingNonExistent() {
            when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.delete(taskId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(taskId.toString());

            verify(taskRepository).findById(taskId);
            verify(taskRepository, never()).delete(any());
            verifyNoMoreInteractions(taskRepository, taskMapper);
        }
    }
}
