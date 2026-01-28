package com.portfolio.api.dto.response;

public record ValidationError(
        String field,
        String message
) {
}
