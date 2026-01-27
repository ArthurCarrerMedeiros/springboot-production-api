package com.portfolio.api.exception;

public record ValidationError(
        String field,
        String message
) {}
