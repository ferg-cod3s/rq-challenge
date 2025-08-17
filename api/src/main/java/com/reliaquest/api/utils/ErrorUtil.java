// api/src/main/java/com/reliaquest/api/utils/RateLimitHelper.java
package com.reliaquest.api.utils;

import java.time.Duration;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class ErrorUtil {
    private static final Logger log = LoggerFactory.getLogger(ErrorUtil.class);

    /**
     * Returns a function that handles rate limit errors for WebClient .onStatus() calls.
     */
    public static Function<ClientResponse, Mono<? extends Throwable>> handleRateLimit(String message) {
        return r -> r.createException().flatMap(e -> {
            log.warn("Rate Limit: {}", message, e);
            return Mono.error(e);
        });
    }

    /**
     * Returns a function that handles not found errors for WebClient .onStatus() calls.
     */
    public static Function<ClientResponse, Mono<? extends Throwable>> handleNotFound(String message) {
        return r -> r.createException().flatMap(e -> {
            log.warn("Not Found: {}", message, e);
            return Mono.error(e);
        });
    }

    /**
     * Returns a function that handles bad request errors for WebClient .onStatus() calls.
     */
    public static Function<ClientResponse, Mono<? extends Throwable>> handleBadRequest(String message) {
        return r -> r.createException().flatMap(e -> {
            log.warn("Bad Request: {}", message, e);
            return Mono.error(e);
        });
    }

    /**
     * Returns a function that handles generic errors for WebClient .onStatus() calls.
     */
    public static Function<ClientResponse, Mono<? extends Throwable>> handleError(String message) {
        return r -> r.createException().flatMap(e -> {
            log.error("Error: {}", message, e);
            return Mono.error(e);
        });
    }

    /**
     * Returns a standardized retry configuration for rate-limited API calls.
     * Uses adaptive backoff strategy to handle the mock server's random rate limiting behavior.
     * The mock server randomly chooses when to rate-limit, so we need to handle this properly.
     * Strategy:
     * - 4 retry attempts to handle multiple consecutive rate limits
     * - Exponential backoff starting at 1 second, growing to handle longer server backoffs
     * - Max delay of 120 seconds to accommodate worst-case server backoff (90s + buffer)
     * - Only retries on 429 TOO_MANY_REQUESTS errors
     */
    public static Retry rateLimitRetry() {
        return Retry.backoff(4, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(120))
                .filter(ex -> ex instanceof WebClientResponseException
                        && ((WebClientResponseException) ex).getStatusCode() == HttpStatus.TOO_MANY_REQUESTS);
    }
}
