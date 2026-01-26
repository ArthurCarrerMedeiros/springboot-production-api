package com.portfolio.api.repository;

import com.portfolio.api.model.Task;
import com.portfolio.api.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
}
