package com.ist.internal_issue_tracker.shared.security;

import tools.jackson.databind.ObjectMapper;
import com.ist.internal_issue_tracker.shared.exception.CommonErrorCode;
import com.ist.internal_issue_tracker.shared.web.ApiError;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs inside the security filter chain, before {@code DispatcherServlet} -
 * an unauthenticated request never reaches {@code GlobalExceptionHandler},
 * so the {@code ApiResponse} envelope has to be written here directly.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ApiError error = ApiError.of(
                CommonErrorCode.UNAUTHENTICATED.code(), CommonErrorCode.UNAUTHENTICATED.defaultMessage(), request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(error));
    }
}
