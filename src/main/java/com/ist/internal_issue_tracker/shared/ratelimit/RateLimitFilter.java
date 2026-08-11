package com.ist.internal_issue_tracker.shared.ratelimit;

import com.ist.internal_issue_tracker.shared.exception.CommonErrorCode;
import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.web.ApiError;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Runs after {@link com.ist.internal_issue_tracker.shared.security.JwtAuthenticationFilter}, so
 * the SecurityContext is already populated when this checks for an authenticated principal.
 *
 * <p>The two buckets are exclusive, not stacked: an authenticated request is checked only against
 * its user bucket, never the IP one. Sharing a single IP bucket across every authenticated request
 * behind that IP would let an office NAT/proxy exhaust it for everyone on it - the whole point of a
 * per-user bucket is that it already scopes to one person regardless of who else shares the
 * address. The IP bucket exists for requests that have no user yet (login, register).
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private final RateLimiterService rateLimiterService;
  private final ObjectMapper objectMapper;

  public RateLimitFilter(RateLimiterService rateLimiterService, ObjectMapper objectMapper) {
    this.rateLimiterService = rateLimiterService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean allowed;
    if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
      allowed = rateLimiterService.tryConsume("user:" + user.getId(), RateLimiterService.perUser());
    } else {
      allowed = rateLimiterService.tryConsume("ip:" + clientIp(request), RateLimiterService.perIp());
    }

    if (!allowed) {
      writeRateLimitedResponse(request, response);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private void writeRateLimitedResponse(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ApiError error =
        ApiError.of(
            CommonErrorCode.RATE_LIMITED.code(),
            CommonErrorCode.RATE_LIMITED.defaultMessage(),
            request.getRequestURI());

    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    // saniye cinsinden sabit; perIp/perUser'daki pencerelerle eşleşiyor
    response.setHeader(HttpHeaders.RETRY_AFTER, "60");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), ApiResponse.error(error));
  }

  // X-Forwarded-For'un ilki gerçek istemci; proxy'ler kendi IP'lerini sona ekler
  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
  }
}
