package com.ist.internal_issue_tracker.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Machine-readable error payload returned inside {@link ApiResponse#error()}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        @Nullable String path,
        @Nullable List<FieldError> fieldErrors,
        @Nullable String traceId
) {

    /** One violation on a single request field or, for class-level constraints, the object name. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldError(String field, String message) {
    }

    public static ApiError of(String code, String message, @Nullable String path) {
        return new ApiError(code, message, path, null, null);
    }

    public static ApiError validation(String message, @Nullable String path, List<FieldError> fieldErrors) {
        return new ApiError("VALIDATION_FAILED", message, path, fieldErrors, null);
    }

    public static ApiError internal(String traceId, @Nullable String path) {
        return new ApiError(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please contact support with reference " + traceId + ".",
                path, null, traceId);
    }
}
