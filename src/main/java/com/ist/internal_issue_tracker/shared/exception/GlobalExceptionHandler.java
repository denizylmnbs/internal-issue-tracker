package com.ist.internal_issue_tracker.shared.exception;

import com.ist.internal_issue_tracker.shared.web.ApiError;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The single place every exception in this application funnels through.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} rather than being a bare
 * {@code @RestControllerAdvice}: the base class already handles ~20 Spring MVC exceptions (wrong
 * HTTP method, unsupported media type, malformed JSON, unmatched route, ...) with the correct
 * status and headers (e.g. {@code Allow} on 405). A bare advice would let every one of those fall
 * through to Boot's default {@code BasicErrorController} shape, silently breaking the single
 * response envelope this class exists to guarantee.
 *
 * <p>Not covered here: exceptions raised in the security filter chain (before {@code
 * DispatcherServlet}), e.g. an unauthenticated request hitting a protected endpoint. Those never
 * reach a {@code @RestControllerAdvice} and must be wired via a custom {@code
 * AuthenticationEntryPoint} / {@code AccessDeniedHandler} once {@code SecurityConfig} exists.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // ---------------------------------------------------------------------
  // Universal reshaping hook: every path in the base class funnels through
  // createResponseEntity, so this single override converts all inherited
  // framework exceptions into the ApiResponse envelope.
  // ---------------------------------------------------------------------

  private static ErrorCode mapStatusToCommonCode(HttpStatusCode statusCode) {
    if (statusCode.isSameCodeAs(HttpStatus.METHOD_NOT_ALLOWED)) {
      return CommonErrorCode.METHOD_NOT_ALLOWED;
    }
    if (statusCode.isSameCodeAs(HttpStatus.NOT_ACCEPTABLE)) {
      return CommonErrorCode.NOT_ACCEPTABLE;
    }
    if (statusCode.isSameCodeAs(HttpStatus.UNSUPPORTED_MEDIA_TYPE)) {
      return CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
    }
    if (statusCode.isSameCodeAs(HttpStatus.NOT_FOUND)) {
      return CommonErrorCode.ENDPOINT_NOT_FOUND;
    }
    if (statusCode.isSameCodeAs(HttpStatus.PAYLOAD_TOO_LARGE)) {
      return CommonErrorCode.PAYLOAD_TOO_LARGE;
    }
    if (statusCode.is5xxServerError()) {
      return CommonErrorCode.INTERNAL_ERROR;
    }
    // remaining framework 4xx cases (bad JSON, conversion failures, async timeout, ...)
    return CommonErrorCode.MALFORMED_REQUEST;
  }

  private static String safeMessage(String message, String fallbackCode) {
    if (message != null) {
      return message;
    }
    return fallbackCode != null ? fallbackCode : "is invalid";
  }

  private static String lastNode(String propertyPath) {
    int idx = propertyPath.lastIndexOf('.');
    return idx >= 0 ? propertyPath.substring(idx + 1) : propertyPath;
  }

  private static String path(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return servletWebRequest.getRequest().getRequestURI();
    }
    return request.getDescription(false);
  }

  // ---------------------------------------------------------------------
  // Overrides that need field-level detail ProblemDetail alone loses.
  // ---------------------------------------------------------------------

  private static String newTraceId() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  @Override
  protected ResponseEntity<Object> createResponseEntity(
      Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

    Object payload =
        switch (body) {
          case ApiResponse<?> already -> already;
          case ProblemDetail pd -> ApiResponse.error(fromProblemDetail(pd, statusCode, request));
          case null -> ApiResponse.error(fromStatus(statusCode, request));
          default -> body;
        };
    return super.createResponseEntity(payload, headers, statusCode, request);
  }

  private ApiError fromProblemDetail(
      ProblemDetail pd, HttpStatusCode statusCode, WebRequest request) {
    ErrorCode code = mapStatusToCommonCode(statusCode);
    String message = pd.getDetail() != null ? pd.getDetail() : code.defaultMessage();
    return ApiError.of(code.code(), message, path(request));
  }

  private ApiError fromStatus(HttpStatusCode statusCode, WebRequest request) {
    ErrorCode code = mapStatusToCommonCode(statusCode);
    return ApiError.of(code.code(), code.defaultMessage(), path(request));
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    List<ApiError.FieldError> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe ->
                    new ApiError.FieldError(
                        fe.getField(), safeMessage(fe.getDefaultMessage(), fe.getCode())))
            .toList();
    List<ApiError.FieldError> globalErrors =
        ex.getBindingResult().getGlobalErrors().stream()
            .map(
                oe ->
                    new ApiError.FieldError(
                        oe.getObjectName(), safeMessage(oe.getDefaultMessage(), oe.getCode())))
            .toList();

    List<ApiError.FieldError> merged =
        Stream.concat(fieldErrors.stream(), globalErrors.stream())
            .sorted(
                Comparator.comparing(ApiError.FieldError::field)
                    .thenComparing(ApiError.FieldError::message))
            .toList();

    log.debug("Validation failed at {}: {}", path(request), merged);
    ApiError error =
        ApiError.validation(
            CommonErrorCode.VALIDATION_FAILED.defaultMessage(), path(request), merged);
    return handleExceptionInternal(
        ex, ApiResponse.error(error), headers, HttpStatus.BAD_REQUEST, request);
  }

  // ---------------------------------------------------------------------
  // Exceptions the base class does not know about.
  // ---------------------------------------------------------------------

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    List<ApiError.FieldError> fieldErrors =
        ex.getParameterValidationResults().stream()
            .flatMap(this::toFieldErrors)
            .sorted(
                Comparator.comparing(ApiError.FieldError::field)
                    .thenComparing(ApiError.FieldError::message))
            .toList();

    log.debug("Parameter validation failed at {}: {}", path(request), fieldErrors);
    ApiError error =
        ApiError.validation(
            CommonErrorCode.VALIDATION_FAILED.defaultMessage(), path(request), fieldErrors);
    return handleExceptionInternal(
        ex, ApiResponse.error(error), headers, HttpStatus.BAD_REQUEST, request);
  }

  private Stream<ApiError.FieldError> toFieldErrors(ParameterValidationResult result) {
    String field = result.getMethodParameter().getParameterName();
    String resolvedField = field != null ? field : "parameter";
    return result.getResolvableErrors().stream()
        .map(e -> new ApiError.FieldError(resolvedField, safeMessage(e.getDefaultMessage(), null)));
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

    String property = ex.getPropertyName() != null ? ex.getPropertyName() : "parameter";
    String requiredType =
        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "a different type";
    String message =
        "'"
            + ex.getValue()
            + "' is not a valid value for '"
            + property
            + "' (expected "
            + requiredType
            + ")";

    log.debug("Type mismatch at {}: {}", path(request), message);
    ApiError error = ApiError.of(CommonErrorCode.TYPE_MISMATCH.code(), message, path(request));
    return handleExceptionInternal(
        ex, ApiResponse.error(error), headers, HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(AppException.class)
  ResponseEntity<ApiResponse<Void>> handleApp(AppException ex, WebRequest request) {
    HttpStatus status = ex.status();
    if (status.is5xxServerError()) {
      // An AppException mapped to a 5xx status is a mapping bug in the throwing code,
      // not an expected outcome - treat it like the catch-all: log fully, leak nothing.
      String traceId = newTraceId();
      log.error("[{}] AppException with 5xx code {} at {}", traceId, ex.code(), path(request), ex);
      return ResponseEntity.status(status)
          .body(ApiResponse.error(ApiError.internal(traceId, path(request))));
    }
    log.debug("{} at {}: {}", ex.code(), path(request), ex.getMessage());
    return ResponseEntity.status(status)
        .body(ApiResponse.error(ApiError.of(ex.code(), ex.getMessage(), path(request))));
  }

  /** {@code @Validated} on a {@code @Service}/{@code @Component} method (not a controller). */
  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
      ConstraintViolationException ex, WebRequest request) {
    List<ApiError.FieldError> fieldErrors =
        ex.getConstraintViolations().stream()
            .map(
                v ->
                    new ApiError.FieldError(
                        lastNode(v.getPropertyPath().toString()), v.getMessage()))
            .sorted(
                Comparator.comparing(ApiError.FieldError::field)
                    .thenComparing(ApiError.FieldError::message))
            .toList();
    log.debug("Constraint violation at {}: {}", path(request), fieldErrors);
    ApiError error =
        ApiError.validation(
            CommonErrorCode.VALIDATION_FAILED.defaultMessage(), path(request), fieldErrors);
    return ResponseEntity.badRequest().body(ApiResponse.error(error));
  }

  /**
   * Safety net for a unique-index race that slipped past a service-layer pre-check. Deliberately
   * generic in v1: mapping specific constraint names (e.g. {@code
   * unique_active_sprint_name_per_project}) to module-specific codes would require shared to know
   * module vocabulary. Services should throw {@link DuplicateResourceException}/{@link
   * ConflictException} explicitly for the expected case; this only catches races.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
      DataIntegrityViolationException ex, WebRequest request) {
    log.warn("Data integrity violation at {}", path(request), ex);
    ApiError error =
        ApiError.of(
            CommonErrorCode.CONFLICT.code(),
            CommonErrorCode.CONFLICT.defaultMessage(),
            path(request));
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ResponseEntity<ApiResponse<Void>> handleOptimisticLocking(
      OptimisticLockingFailureException ex, WebRequest request) {
    log.warn("Optimistic locking failure at {}", path(request));
    ApiError error =
        ApiError.of(
            CommonErrorCode.CONFLICT.code(),
            "The resource was modified concurrently, please retry",
            path(request));
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
  }

  /**
   * Only reached for access decisions made inside dispatch (e.g. method security via
   * {@code @PreAuthorize}). {@code AccessDeniedException} thrown by the filter chain never reaches
   * this advice - see class Javadoc. An explicit handler is mandatory here: without it, this
   * exception falls through to {@link #handleUnexpected}, turning a 403 into a silent 500.
   */
  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiResponse<Void>> handleAccessDenied(
      AccessDeniedException ex, WebRequest request) {
    log.warn("Access denied at {}", path(request));
    ApiError error =
        ApiError.of(
            CommonErrorCode.FORBIDDEN.code(),
            CommonErrorCode.FORBIDDEN.defaultMessage(),
            path(request));
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(error));
  }

  // ---------------------------------------------------------------------

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ApiResponse<Void>> handleAuthentication(
      AuthenticationException ex, WebRequest request) {
    log.warn("Authentication failed at {}", path(request));
    ApiError error =
        ApiError.of(
            CommonErrorCode.UNAUTHENTICATED.code(),
            CommonErrorCode.UNAUTHENTICATED.defaultMessage(),
            path(request));
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error));
  }

  /**
   * Last resort. Never leak {@code ex.getMessage()} - it routinely contains SQL, table names, or
   * connection strings.
   */
  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, WebRequest request) {
    String traceId = newTraceId();
    log.error("[{}] Unhandled exception at {}", traceId, path(request), ex);
    return ResponseEntity.internalServerError()
        .body(ApiResponse.error(ApiError.internal(traceId, path(request))));
  }
}
