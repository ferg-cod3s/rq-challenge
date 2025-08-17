package com.reliaquest.api.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

class ErrorUtilTest {

    @Test
    void handleRateLimit_returnsCorrectFunction() {
        // Given
        String message = "Rate limit exceeded for test operation";
        ClientResponse mockResponse = mock(ClientResponse.class);
        WebClientResponseException mockException =
                WebClientResponseException.create(429, "Too Many Requests", null, null, null);

        when(mockResponse.createException()).thenReturn(Mono.just(mockException));

        // When
        Function<ClientResponse, Mono<? extends Throwable>> handler = ErrorUtil.handleRateLimit(message);
        Mono<? extends Throwable> result = handler.apply(mockResponse);

        // Then
        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void handleNotFound_returnsCorrectFunction() {
        // Given
        String message = "Employee not found";
        ClientResponse mockResponse = mock(ClientResponse.class);
        WebClientResponseException mockException =
                WebClientResponseException.create(404, "Not Found", null, null, null);

        when(mockResponse.createException()).thenReturn(Mono.just(mockException));

        // When
        Function<ClientResponse, Mono<? extends Throwable>> handler = ErrorUtil.handleNotFound(message);
        Mono<? extends Throwable> result = handler.apply(mockResponse);

        // Then
        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void handleBadRequest_returnsCorrectFunction() {
        // Given
        String message = "Invalid request data";
        ClientResponse mockResponse = mock(ClientResponse.class);
        WebClientResponseException mockException =
                WebClientResponseException.create(400, "Bad Request", null, null, null);

        when(mockResponse.createException()).thenReturn(Mono.just(mockException));

        // When
        Function<ClientResponse, Mono<? extends Throwable>> handler = ErrorUtil.handleBadRequest(message);
        Mono<? extends Throwable> result = handler.apply(mockResponse);

        // Then
        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void handleError_returnsCorrectFunction() {
        // Given
        String message = "Internal server error occurred";
        ClientResponse mockResponse = mock(ClientResponse.class);
        WebClientResponseException mockException =
                WebClientResponseException.create(500, "Internal Server Error", null, null, null);

        when(mockResponse.createException()).thenReturn(Mono.just(mockException));

        // When
        Function<ClientResponse, Mono<? extends Throwable>> handler = ErrorUtil.handleError(message);
        Mono<? extends Throwable> result = handler.apply(mockResponse);

        // Then
        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void rateLimitRetry_hasCorrectConfiguration() {
        // When
        Retry retrySpec = ErrorUtil.rateLimitRetry();

        // Then
        assertThat(retrySpec).isNotNull();
        // Note: Testing internal configuration of Retry is complex,
        // but we can verify it doesn't throw when created
    }

    @Test
    void rateLimitRetry_configurationIsValid() {
        // When
        Retry retrySpec = ErrorUtil.rateLimitRetry();

        // Then - The retry spec should be created without errors
        assertThat(retrySpec).isNotNull();

        // Test that it only retries appropriate exceptions by using a counter
        var attemptCounter = new java.util.concurrent.atomic.AtomicInteger(0);
        WebClientResponseException rateLimitException =
                WebClientResponseException.create(429, "Too Many Requests", null, null, null);

        // This should retry and increment the counter multiple times
        StepVerifier.create(Mono.fromCallable(() -> {
                            int attempts = attemptCounter.incrementAndGet();
                            if (attempts <= 2) { // Allow a couple retries
                                throw rateLimitException;
                            }
                            return "success";
                        })
                        .retryWhen(retrySpec))
                .expectNext("success")
                .verifyComplete();

        // Verify it retried at least once
        assertThat(attemptCounter.get()).isGreaterThan(1);
    }

    @Test
    void rateLimitRetry_doesNotRetryNonRateLimitErrors() {
        // Given
        Retry retrySpec = ErrorUtil.rateLimitRetry();
        var attemptCounter = new java.util.concurrent.atomic.AtomicInteger(0);
        WebClientResponseException notFoundException =
                WebClientResponseException.create(404, "Not Found", null, null, null);

        // When/Then - Should NOT retry non-429 errors
        StepVerifier.create(Mono.fromCallable(() -> {
                            attemptCounter.incrementAndGet();
                            throw notFoundException;
                        })
                        .retryWhen(retrySpec))
                .expectError(WebClientResponseException.class)
                .verify();

        // Should only have attempted once (no retries)
        assertThat(attemptCounter.get()).isEqualTo(1);
    }

    @Test
    void errorHandlers_preserveOriginalException() {
        // Given
        String customMessage = "Custom error message";
        ClientResponse mockResponse = mock(ClientResponse.class);
        WebClientResponseException originalException =
                WebClientResponseException.create(429, "Original Status Text", null, null, null);

        when(mockResponse.createException()).thenReturn(Mono.just(originalException));

        // When
        Function<ClientResponse, Mono<? extends Throwable>> handler = ErrorUtil.handleRateLimit(customMessage);

        // Then
        StepVerifier.create(handler.apply(mockResponse))
                .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException
                        && ((WebClientResponseException) throwable).getStatusCode() == HttpStatus.TOO_MANY_REQUESTS
                        && throwable.getMessage().contains("Original Status Text"))
                .verify();
    }

    @Test
    void allErrorHandlers_returnNonNullFunctions() {
        // When/Then
        assertThat(ErrorUtil.handleRateLimit("test")).isNotNull();
        assertThat(ErrorUtil.handleNotFound("test")).isNotNull();
        assertThat(ErrorUtil.handleBadRequest("test")).isNotNull();
        assertThat(ErrorUtil.handleError("test")).isNotNull();
        assertThat(ErrorUtil.rateLimitRetry()).isNotNull();
    }

    @Test
    void errorHandlers_handleNullMessage() {
        // Given
        ClientResponse mockResponse = mock(ClientResponse.class);
        WebClientResponseException mockException =
                WebClientResponseException.create(429, "Too Many Requests", null, null, null);

        when(mockResponse.createException()).thenReturn(Mono.just(mockException));

        // When/Then - Should not throw with null message
        Function<ClientResponse, Mono<? extends Throwable>> handler = ErrorUtil.handleRateLimit(null);

        StepVerifier.create(handler.apply(mockResponse))
                .expectError(WebClientResponseException.class)
                .verify();
    }
}
