package com.ist.internal_issue_tracker.shared.security;

import tools.jackson.databind.ObjectMapper;
import com.ist.internal_issue_tracker.shared.exception.CommonErrorCode;
import com.ist.internal_issue_tracker.shared.web.ApiError;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs inside the security filter chain - an authenticated-but-unauthorized
 * request never reaches {@code GlobalExceptionHandler}, so the {@code ApiResponse}
 * envelope has to be written here directly (mirrors {@link RestAuthenticationEntryPoint}).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        ApiError error = ApiError.of(
                CommonErrorCode.FORBIDDEN.code(), CommonErrorCode.FORBIDDEN.defaultMessage(), request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(error));
    }
}
