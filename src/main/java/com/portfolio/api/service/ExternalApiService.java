package com.portfolio.api.service;

import com.portfolio.api.dto.response.PingResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Service
public class ExternalApiService {

    private final WebClient webClient;
    private final Duration timeout;

    public ExternalApiService(@Value("${app.external-api.base-url}") String baseUrl,
                               @Value("${app.external-api.timeout-seconds:3}") int timeoutSeconds) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @CircuitBreaker(name = "externalApi", fallbackMethod = "pingFallback")
    @Retry(name = "externalApi")
    public PingResponse ping() {
        log.debug("Calling external API...");

        ResponseEntity<Void> response = webClient.get()
                .uri("/get")
                .retrieve()
                .toBodilessEntity()
                .timeout(timeout)
                .block();

        if (response == null) {
            throw new IllegalStateException("External API returned null response");
        }

        int status = response.getStatusCode().value();
        return new PingResponse("up", "External service responded with " + status);
    }

    private PingResponse pingFallback(Throwable t) {
        log.warn("External API unavailable, using fallback. type={}, reason={}", t.getClass().getName(), t.getMessage());
        return new PingResponse("degraded", "External service unavailable");
    }
}
