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
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Runs after {@link com.ist.internal_issue_tracker.shared.security.JwtAuthenticationFilter}, so the
 * SecurityContext is already populated when this checks for an authenticated principal.
 *
 * <p>The two buckets are exclusive, not stacked: an authenticated request is checked only against
 * its user bucket, never the IP one. Sharing a single IP bucket across every authenticated request
 * behind that IP would let an office NAT/proxy exhaust it for everyone on it - the whole point of a
 * per-user bucket is that it already scopes to one person regardless of who else shares the
 * address. The IP bucket exists for requests that have no user yet (login, register).
 *
 * <p>File uploads get a third, much tighter bucket. It is keyed separately ({@code upload:} rather
 * than {@code user:}) and that is not cosmetic: Bucket4j stores the configuration alongside the
 * bucket, so pointing two different {@code Bandwidth}s at one key would make every request
 * reconfigure the bucket the previous one just set up, silently destroying both limits. The
 * side effect of separate keys is that an upload does not also draw down the general bucket -
 * acceptable, since the upload limit is by far the stricter of the two.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
  private static final String AVATAR_UPLOAD_PATTERN = "/api/users/*/avatar";

  private final RateLimiterService rateLimiterService;
  private final ObjectMapper objectMapper;
  private final Set<String> trustedProxies;

  public RateLimitFilter(
      RateLimiterService rateLimiterService,
      ObjectMapper objectMapper,
      Set<String> trustedProxies) {
    this.rateLimiterService = rateLimiterService;
    this.objectMapper = objectMapper;
    this.trustedProxies = trustedProxies;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean allowed;
    if (authentication != null
        && authentication.getPrincipal() instanceof AuthenticatedUser user) {
      allowed =
          isUpload(request)
              ? rateLimiterService.tryConsume(
                  "upload:" + user.getId(), RateLimiterService.perUpload())
              : rateLimiterService.tryConsume("user:" + user.getId(), RateLimiterService.perUser());
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

  private static boolean isUpload(HttpServletRequest request) {
    return "PUT".equals(request.getMethod())
        && PATH_MATCHER.match(AVATAR_UPLOAD_PATTERN, request.getRequestURI());
  }

  /**
   * X-Forwarded-For'un ilki gerçek istemci; proxy'ler kendi IP'lerini sona ekler. Ama bu başlığı
   * herkes gönderebilir: koşulsuz güvenilirse her istekte farklı bir değer uydurmak IP kovasını
   * tamamen etkisiz hale getirir (kova anahtarı değişince limit hiç dolmaz). Bu yüzden başlık
   * yalnızca istek gerçekten güvenilen bir ters proxy'den geliyorsa okunur.
   *
   * <p>Varsayılan liste boş, yani XFF varsayılan olarak hiç dikkate alınmaz - spoofing kapalı
   * doğar. Bunun bedeli: prod'da app.security.trusted-proxies ayarlanmazsa proxy arkasındaki tüm
   * kimliksiz trafik proxy'nin tek IP'sinde toplanır ve login limiti herkes için ortak olur.
   */
  private String clientIp(HttpServletRequest request) {
    String remoteAddr = request.getRemoteAddr();
    if (trustedProxies.contains(remoteAddr)) {
      String forwarded = request.getHeader("X-Forwarded-For");
      if (forwarded != null && !forwarded.isBlank()) {
        return forwarded.split(",")[0].trim();
      }
    }
    return remoteAddr;
  }
}
