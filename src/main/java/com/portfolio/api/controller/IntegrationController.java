package com.portfolio.api.controller;

import com.portfolio.api.dto.response.PingResponse;
import com.portfolio.api.service.ExternalApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
@Tag(name = "Integrations", description = "External service integration endpoints")
public class IntegrationController {

    private final ExternalApiService externalApiService;

    @GetMapping("/ping")
    @Operation(summary = "Ping external service", description = "Tests connectivity to external service with circuit breaker and retry")
    public ResponseEntity<PingResponse> ping() {
        PingResponse response = externalApiService.ping();

        if ("degraded".equals(response.status())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
