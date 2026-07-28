package com.ist.internal_issue_tracker.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the {@code Authorization: Bearer <token>} header, if present, and populates the {@code
 * SecurityContext}. A missing or unparsable header, or a userId that no longer resolves via {@link
 * AuthenticatedUserLookup}, just leaves the request unauthenticated - it is up to {@code
 * SecurityConfig}'s authorization rules (and {@link RestAuthenticationEntryPoint}) to reject it
 * later if the endpoint requires authentication.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final AuthenticatedUserLookup authenticatedUserLookup;

  public JwtAuthenticationFilter(
      JwtService jwtService, AuthenticatedUserLookup authenticatedUserLookup) {
    this.jwtService = jwtService;
    this.authenticatedUserLookup = authenticatedUserLookup;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String header = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (header != null && header.startsWith(BEARER_PREFIX)) {
      String token = header.substring(BEARER_PREFIX.length());
      try {
        Integer userId = jwtService.extractUserId(token);
        Optional<AuthenticatedUser> authenticatedUser = authenticatedUserLookup.findById(userId);

        if (authenticatedUser.isPresent()) {
          // Only the user's own role is granted; the implied lower roles are resolved at decision
          // time by the RoleHierarchy bean in SecurityConfig.
          List<GrantedAuthority> authorities =
              List.of(new SimpleGrantedAuthority(authenticatedUser.get().getRole().authority()));

          var authentication =
              new UsernamePasswordAuthenticationToken(authenticatedUser.get(), null, authorities);
          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
          SecurityContextHolder.clearContext();
        }
      } catch (RuntimeException ex) {
        log.debug(
            "Rejecting invalid bearer token at {}: {}", request.getRequestURI(), ex.getMessage());
        SecurityContextHolder.clearContext();
      }
    }

    filterChain.doFilter(request, response);
  }
}
