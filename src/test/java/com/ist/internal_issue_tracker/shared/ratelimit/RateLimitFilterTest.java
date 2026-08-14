package com.ist.internal_issue_tracker.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.security.Role;
import io.github.bucket4j.Bandwidth;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

/**
 * Covers the two decisions the filter makes before it ever touches Redis: which bucket a request
 * belongs to, and what counts as its client address. Both are security-relevant and neither needs a
 * servlet container or a running Redis to exercise.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

  @Mock private RateLimiterService rateLimiterService;
  @Mock private FilterChain filterChain;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private static void authenticateAs(int userId) {
    AuthenticatedUser principal = new AuthenticatedUser(userId, Role.DEVELOPER);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
  }

  /** Runs one request through the filter with the limiter always allowing it. */
  private void run(Set<String> trustedProxies, MockHttpServletRequest request) throws Exception {
    when(rateLimiterService.tryConsume(anyString(), any())).thenReturn(true);
    new RateLimitFilter(rateLimiterService, objectMapper, trustedProxies)
        .doFilter(request, new MockHttpServletResponse(), filterChain);
  }

  @Test
  void avatarUploadUsesItsOwnBucketKeyAndBandwidth() throws Exception {
    // Separate key, not just a separate Bandwidth: Bucket4j stores the configuration alongside the
    // bucket, so two Bandwidths on one key would reconfigure it on every request and silently
    // destroy both limits.
    authenticateAs(7);

    run(Set.of(), new MockHttpServletRequest("PUT", "/api/users/7/avatar"));

    ArgumentCaptor<Bandwidth> bandwidth = ArgumentCaptor.forClass(Bandwidth.class);
    verify(rateLimiterService).tryConsume(eq("upload:7"), bandwidth.capture());
    assertThat(bandwidth.getValue().getCapacity()).isEqualTo(10);
  }

  @Test
  void ordinaryAuthenticatedRequestsStayOnTheGeneralBucket() throws Exception {
    authenticateAs(7);

    run(Set.of(), new MockHttpServletRequest("GET", "/api/issues"));

    verify(rateLimiterService).tryConsume(eq("user:7"), any());
  }

  @Test
  void aGetOnTheAvatarPathIsNotAnUpload() throws Exception {
    // Only the multipart PUT carries a decode and an AV round trip; reading is ordinary traffic.
    authenticateAs(7);

    run(Set.of(), new MockHttpServletRequest("GET", "/api/users/7/avatar"));

    verify(rateLimiterService).tryConsume(eq("user:7"), any());
  }

  @Test
  void forwardedForIsIgnored_whenTheRequestDoesNotComeFromATrustedProxy() throws Exception {
    // Anyone can send this header. Believed unconditionally, a client could hand itself a fresh
    // bucket key on every request and never hit the limit at all.
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.setRemoteAddr("203.0.113.9");
    request.addHeader("X-Forwarded-For", "1.2.3.4");

    run(Set.of(), request);

    verify(rateLimiterService).tryConsume(eq("ip:203.0.113.9"), any());
  }

  @Test
  void forwardedForIsHonored_whenTheRequestComesFromATrustedProxy() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.setRemoteAddr("10.0.0.1");
    request.addHeader("X-Forwarded-For", "1.2.3.4, 10.0.0.1");

    run(Set.of("10.0.0.1"), request);

    verify(rateLimiterService).tryConsume(eq("ip:1.2.3.4"), any());
  }

  @Test
  void trustedProxyWithoutTheHeaderFallsBackToTheSocketAddress() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.setRemoteAddr("10.0.0.1");

    run(Set.of("10.0.0.1"), request);

    verify(rateLimiterService).tryConsume(eq("ip:10.0.0.1"), any());
  }
}
