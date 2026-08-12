package com.ist.internal_issue_tracker.shared.security;

import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import com.ist.internal_issue_tracker.shared.ratelimit.RateLimitFilter;
import com.ist.internal_issue_tracker.shared.ratelimit.RateLimiterService;
import java.util.List;
import java.util.function.BiPredicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * The single source of truth for role precedence. Declaring it as a bean is enough: {@code
   * authorizeHttpRequests} picks it up on its own, so a rule written as {@code
   * hasRole("DEVELOPER")} also admits editors and admins and each endpoint only has to state its
   * <em>minimum</em> role.
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

  /**
   * Browser access for the separately deployed frontend. Without this every cross-origin request
   * fails at the preflight, including the login - a server-side client like {@code curl} never sees
   * the difference, which is what makes the omission easy to miss.
   *
   * <p>Origins are configured rather than wildcarded because {@code allowCredentials} forbids
   * {@code "*"}, and because a wildcard on an internal tracker would let any page on the internet
   * make authenticated requests on a logged-in user's behalf if the token ever moves to a cookie.
   * {@code CORS_ALLOWED_ORIGINS} takes a comma-separated list; the default covers local development
   * against a Next.js dev server.
   *
   * <p>{@code Location} is exposed because the create endpoints return it and a client that cannot
   * read it has to re-fetch to learn the new resource's URL.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
    configuration.setExposedHeaders(List.of(HttpHeaders.LOCATION));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);

    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtService jwtService,
      AuthenticatedUserLookup authenticatedUserLookup,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler,
      RoleHierarchy roleHierarchy,
      TeamLookup teamLookup,
      ProjectLookup projectLookup,
      CorsConfigurationSource corsConfigurationSource,
      RateLimiterService rateLimiterService,
      ObjectMapper objectMapper)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        // Ahead of the authorization rules on purpose: a CORS preflight carries no Authorization
        // header, so an OPTIONS request would be rejected before the browser ever learns the real
        // request was allowed.
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
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
                    .requestMatchers(HttpMethod.POST, "/api/auth/refresh")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/logout")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/users/register")
                    .permitAll()
                    .requestMatchers(HttpMethod.PUT, "/api/users/{id}")
                    .access(selfOrAdmin(roleHierarchy))
                    .requestMatchers(HttpMethod.DELETE, "/api/users/{id}")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/users/{id}/password")
                    .access(selfOrAdmin(roleHierarchy))
                    .requestMatchers(HttpMethod.POST, "/api/users/{id}/reset-password")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/users/{id}/role")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/users/{id}/issues")
                    .access(selfOrAdmin(roleHierarchy))
                    .requestMatchers(HttpMethod.GET, "/api/users/{id}/sprint-progress")
                    .access(selfOrAdmin(roleHierarchy))
                    .requestMatchers(HttpMethod.POST, "/api/teams")
                    .hasRole("EDITOR")
                    .requestMatchers(HttpMethod.DELETE, "/api/teams/{id}")
                    .hasRole("EDITOR")
                    .requestMatchers(HttpMethod.PATCH, "/api/teams/{id}/leader")
                    .hasRole("EDITOR")
                    .requestMatchers(HttpMethod.PUT, "/api/teams/{id}")
                    .access(editorOrTeamLeader(roleHierarchy, teamLookup))
                    .requestMatchers(HttpMethod.POST, "/api/teams/{id}/members")
                    .access(editorOrTeamLeader(roleHierarchy, teamLookup))
                    .requestMatchers(HttpMethod.DELETE, "/api/teams/{id}/members/{userId}")
                    .access(editorOrTeamLeader(roleHierarchy, teamLookup))
                    .requestMatchers(HttpMethod.POST, "/api/projects")
                    .hasRole("EDITOR")
                    .requestMatchers(HttpMethod.DELETE, "/api/projects/{id}")
                    .hasRole("EDITOR")
                    .requestMatchers(HttpMethod.PATCH, "/api/projects/{id}/leader")
                    .hasRole("EDITOR")
                    .requestMatchers(HttpMethod.DELETE, "/api/projects/{id}/leader")
                    .hasRole("EDITOR")
                    .requestMatchers(HttpMethod.PUT, "/api/projects/{id}")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.PATCH, "/api/projects/{id}/status")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.POST, "/api/projects/{id}/members")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.DELETE, "/api/projects/{id}/members/{userId}")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.POST, "/api/projects/{id}/teams")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.DELETE, "/api/projects/{id}/teams/{teamId}")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.POST, "/api/projects/{id}/sprints")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.PUT, "/api/projects/{id}/sprints/{sprintId}")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(
                        HttpMethod.PATCH, "/api/projects/{id}/sprints/{sprintId}/status")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.DELETE, "/api/projects/{id}/sprints/{sprintId}")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.POST, "/api/projects/{id}/epics")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.PUT, "/api/projects/{id}/epics/{epicId}")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.PATCH, "/api/projects/{id}/epics/{epicId}/status")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.DELETE, "/api/projects/{id}/epics/{epicId}")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    // deleting an issue is the one issue route participants do not get
                    .requestMatchers(HttpMethod.DELETE, "/api/projects/{id}/issues/{issueId}")
                    .access(editorOrProjectLeader(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.POST, "/api/projects/{id}/issues")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.PUT, "/api/projects/{id}/issues/{issueId}")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    // the same gate as the PUT above, because these three are that PUT taken apart
                    .requestMatchers(HttpMethod.PATCH, "/api/projects/{id}/issues/{issueId}/sprint")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.PATCH, "/api/projects/{id}/issues/{issueId}/epic")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    .requestMatchers(
                        HttpMethod.PATCH, "/api/projects/{id}/issues/{issueId}/classification")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    .requestMatchers(HttpMethod.PATCH, "/api/projects/{id}/issues/{issueId}/status")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    .requestMatchers(
                        HttpMethod.PATCH, "/api/projects/{id}/issues/{issueId}/assignee")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    .requestMatchers(
                        HttpMethod.DELETE, "/api/projects/{id}/issues/{issueId}/assignee")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    // the coarse gate only; who may touch a given comment is CommentService's call
                    .requestMatchers(
                        HttpMethod.POST, "/api/projects/{id}/issues/{issueId}/comments")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    .requestMatchers(
                        HttpMethod.PUT, "/api/projects/{id}/issues/{issueId}/comments/{commentId}")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/projects/{id}/issues/{issueId}/comments/{commentId}")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    // The only reads restricted beyond being logged in. Everything else falls
                    // through to authenticated(), which is defensible for a board; a feed of who
                    // changed what and when is not the same kind of thing, so it is held to the
                    // same participation rule as writing.
                    .requestMatchers(HttpMethod.GET, "/api/projects/{id}/activities")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    .requestMatchers(
                        HttpMethod.GET, "/api/projects/{id}/issues/{issueId}/activities")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    .requestMatchers(
                        HttpMethod.GET, "/api/projects/{id}/sprints/{sprintId}/activities")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    // one matcher for all six metric routes; the team that does the work is the one
                    // that has to be able to see its own flow
                    .requestMatchers(HttpMethod.GET, "/api/projects/{id}/metrics/**")
                    .access(editorLeaderOrParticipant(roleHierarchy, projectLookup))
                    // Project-scoped field definitions (the six per-project kinds - see FieldKind).
                    // Reading them is as open as reading the project itself; only EDITOR+ may add,
                    // relabel, reflag, reorder or retire one - see FieldDefinitionController.
                    .requestMatchers(HttpMethod.GET, "/api/projects/{id}/field-definitions")
                    .authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/projects/{id}/field-definitions")
                    .hasRole("EDITOR")
                    .requestMatchers(HttpMethod.PUT, "/api/projects/{id}/field-definitions/**")
                    .hasRole("EDITOR")
                    .requestMatchers(HttpMethod.PATCH, "/api/projects/{id}/field-definitions/**")
                    .hasRole("EDITOR")
                    .requestMatchers(HttpMethod.DELETE, "/api/projects/{id}/field-definitions/**")
                    .hasRole("EDITOR")
                    // The two global kinds (PROJECT_STATUS, TEAM_FIELD) - instance-wide, so writes
                    // are ADMIN-only rather than EDITOR+. The GET rule has to precede the wildcard
                    // below it, or the wildcard would shadow it and require ADMIN just to read.
                    .requestMatchers(HttpMethod.GET, "/api/field-definitions")
                    .authenticated()
                    .requestMatchers("/api/field-definitions/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtService, authenticatedUserLookup),
            UsernamePasswordAuthenticationFilter.class)
        // JwtAuthenticationFilter'dan sonra: SecurityContext doluysa user-id bazlı, boşsa yalnızca
        // IP bazlı limit uygulanır.
        .addFilterAfter(
            new RateLimitFilter(rateLimiterService, objectMapper), JwtAuthenticationFilter.class);

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

      Integer idVariable = parseId(context);
      boolean isSelf = idVariable != null && user.getId().equals(idVariable);

      return new AuthorizationDecision(isSelf || hasRole(roleHierarchy, auth, Role.ADMIN));
    };
  }

  private AuthorizationManager<RequestAuthorizationContext> editorOrTeamLeader(
      RoleHierarchy roleHierarchy, TeamLookup teamLookup) {
    return editorOrLeader(roleHierarchy, teamLookup::isLeaderOfTeam);
  }

  private AuthorizationManager<RequestAuthorizationContext> editorOrProjectLeader(
      RoleHierarchy roleHierarchy, ProjectLookup projectLookup) {
    return editorOrLeader(roleHierarchy, projectLookup::isLeaderOfProject);
  }

  /**
   * The rule the issue routes run on: an editor, the project's leader, or anyone actually working
   * on the project. Sprints and epics are planning artifacts and stay with the first two, but
   * refusing a developer the right to file or move their own work would make the tracker unusable.
   *
   * <p>{@link #editorOrLeader} already takes the leadership question as a predicate, so widening it
   * is a matter of handing it a wider question rather than writing a second manager. Leadership is
   * asked first because it is a single indexed column, while participation is a union across direct
   * and team-based membership.
   */
  private AuthorizationManager<RequestAuthorizationContext> editorLeaderOrParticipant(
      RoleHierarchy roleHierarchy, ProjectLookup projectLookup) {
    return editorOrLeader(
        roleHierarchy,
        (projectId, userId) ->
            projectLookup.isLeaderOfProject(projectId, userId)
                || projectLookup.isParticipantOfProject(projectId, userId));
  }

  /**
   * Shared body of the two "editor, or the person who leads this thing" rules. They differ only in
   * which port answers the leadership question, so {@code isLeader} takes the {@code {id}} path
   * variable and the caller's id and reports whether the second leads the first.
   */
  private AuthorizationManager<RequestAuthorizationContext> editorOrLeader(
      RoleHierarchy roleHierarchy, BiPredicate<Integer, Integer> isLeader) {
    return (authentication, context) -> {
      // take auth information and check if it is valid
      Authentication auth = authentication.get();
      if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
        return new AuthorizationDecision(false);
      }

      // In-memory check first
      if (hasRole(roleHierarchy, auth, Role.EDITOR)) {
        return new AuthorizationDecision(true);
      }

      Integer resourceId = parseId(context);
      if (resourceId == null) {
        return new AuthorizationDecision(false);
      }

      return new AuthorizationDecision(isLeader.test(resourceId, user.getId()));
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

  private Integer parseId(RequestAuthorizationContext context) {
    String raw = context.getVariables().get("id");
    if (raw == null) {
      return null;
    }
    try {
      return Integer.valueOf(raw);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
