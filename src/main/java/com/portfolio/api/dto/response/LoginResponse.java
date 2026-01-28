package com.portfolio.api.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public LoginResponse(String accessToken, long expiresInMinutes) {
        this(accessToken, "Bearer", expiresInMinutes * 60);
    }
}
