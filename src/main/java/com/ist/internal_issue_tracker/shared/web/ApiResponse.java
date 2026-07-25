package com.ist.internal_issue_tracker.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Single response envelope used by every controller in this application.
 * Exactly one of {@code data} / {@code error} is populated depending on
 * {@code success}. HTTP status is never duplicated here — the status line
 * of the response is the single source of truth.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        @Nullable T data,
        @Nullable ApiError error,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    /** For endpoints with no meaningful payload (e.g. delete) that still return the envelope. */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, null, error, Instant.now());
    }
}
