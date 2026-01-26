package com.portfolio.api.dto.request;

import com.portfolio.api.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequest(
        @NotNull(message = "Status is required")
        TaskStatus status
) {}
