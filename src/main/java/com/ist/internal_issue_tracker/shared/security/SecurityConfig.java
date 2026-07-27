package com.ist.internal_issue_tracker.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            AuthenticatedUserLookup authenticatedUserLookup,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/users/{id}/password").access(selfOrAdmin())
                        .requestMatchers(HttpMethod.POST, "/api/users/{id}/reset-password").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, authenticatedUserLookup), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Owner-or-admin check for {@code /api/users/{id}/password}. Kept here rather than
     * as {@code @PreAuthorize} on the controller so every authorization rule for this
     * app lives in one place; {@link RequestAuthorizationContext#getVariables()} gives
     * access to the {@code {id}} path variable the same way SpEL's {@code #id} would.
     */
    private AuthorizationManager<RequestAuthorizationContext> selfOrAdmin() {
        return (authentication, context) -> {
            Authentication auth = authentication.get();
            if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
                return new AuthorizationDecision(false);
            }

            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

            String idVariable = context.getVariables().get("id");
            boolean isSelf = idVariable != null && user.getId().equals(Integer.valueOf(idVariable));

            return new AuthorizationDecision(isAdmin || isSelf);
        };
    }
}
