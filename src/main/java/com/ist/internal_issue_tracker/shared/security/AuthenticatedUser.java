package com.ist.internal_issue_tracker.shared.security;

/**
 * Authentication principal attached to the {@code SecurityContext} by {@link
 * JwtAuthenticationFilter} for every authenticated request. Exposed as a plain class (not a record)
 * so bean-style accessors are guaranteed to resolve in {@code @PreAuthorize} SpEL expressions (e.g.
 * {@code authentication.principal.id}).
 */
public final class AuthenticatedUser {

  private final Integer id;
  private final boolean admin;

  public AuthenticatedUser(Integer id, boolean admin) {
    this.id = id;
    this.admin = admin;
  }

  public Integer getId() {
    return id;
  }

  public boolean isAdmin() {
    return admin;
  }
}
