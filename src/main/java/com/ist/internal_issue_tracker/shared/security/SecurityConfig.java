package com.ist.internal_issue_tracker.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * The single source of truth for role precedence. Declaring it as a bean is enough: {@code
   * authorizeHttpRequests} picks it up on its own, so a rule written as {@code hasRole("DEVELOPER")}
   * also admits editors and admins and each endpoint only has to state its <em>minimum</em> role.
   *
   * <p>The expansion happens at decision time, inside Spring's own authorization managers - it does
   * not change what {@code Authentication#getAuthorities()} returns. Any hand-written check must
   * therefore expand the authorities itself; see {@link #hasRole}.
   */
  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.withDefaultRolePrefix()
        .role("ADMIN")
        .implies("EDITOR")
        .role("EDITOR")
        .implies("DEVELOPER")
        .role("DEVELOPER")
        .implies("USER")
        .build();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtService jwtService,
      AuthenticatedUserLookup authenticatedUserLookup,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler,
      RoleHierarchy roleHierarchy)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()

                    .requestMatchers(HttpMethod.POST, "/api/users/register")
                    .permitAll()

                    .requestMatchers(HttpMethod.PATCH, "/api/users/{id}/password")
                    .access(selfOrAdmin(roleHierarchy))

                    .requestMatchers(HttpMethod.POST, "/api/users/{id}/reset-password")
                    .hasRole("ADMIN")

                    .requestMatchers(HttpMethod.PATCH, "/api/teams/{id}/leader")
                    .hasRole("ADMIN")

                    .anyRequest()
                    .authenticated())
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtService, authenticatedUserLookup),
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Owner-or-admin check for {@code /api/users/{id}/password}. Kept here rather than as
   * {@code @PreAuthorize} on the controller so every authorization rule for this app lives in one
   * place; {@link RequestAuthorizationContext#getVariables()} gives access to the {@code {id}} path
   * variable the same way SpEL's {@code #id} would.
   */
  private AuthorizationManager<RequestAuthorizationContext> selfOrAdmin(
      RoleHierarchy roleHierarchy) {
    return (authentication, context) -> {
      Authentication auth = authentication.get();
      if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
        return new AuthorizationDecision(false);
      }

      String idVariable = context.getVariables().get("id");
      boolean isSelf = idVariable != null && user.getId().equals(Integer.valueOf(idVariable));

      return new AuthorizationDecision(isSelf || hasRole(roleHierarchy, auth, Role.ADMIN));
    };
  }

  /**
   * Role check for hand-written {@link AuthorizationManager}s. It deliberately goes through {@link
   * RoleHierarchy#getReachableGrantedAuthorities} rather than reading {@code auth.getAuthorities()}
   * directly: the authentication only ever carries the user's own role, so a plain comparison would
   * silently ignore the hierarchy and reject users who outrank the required role.
   */
  private boolean hasRole(RoleHierarchy roleHierarchy, Authentication auth, Role required) {
    return roleHierarchy.getReachableGrantedAuthorities(auth.getAuthorities()).stream()
        .anyMatch(authority -> authority.getAuthority().equals(required.authority()));
  }
}
