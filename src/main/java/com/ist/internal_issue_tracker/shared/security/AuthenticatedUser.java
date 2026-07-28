package com.ist.internal_issue_tracker.shared.security;

/**
 * Authentication principal attached to the {@code SecurityContext} by {@link
 * JwtAuthenticationFilter} for every authenticated request. Exposed as a plain class (not a record)
 * so bean-style accessors are guaranteed to resolve in {@code @PreAuthorize} SpEL expressions (e.g.
 * {@code authentication.principal.id}).
 */
public final class AuthenticatedUser {

  private final Integer id;
  private final Role role;

  public AuthenticatedUser(Integer id, Role role) {
    this.id = id;
    this.role = role;
  }

  public Integer getId() {
    return id;
  }

  public Role getRole() {
    return role;
  }
}
