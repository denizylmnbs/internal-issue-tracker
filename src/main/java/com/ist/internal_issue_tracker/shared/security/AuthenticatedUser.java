package com.ist.internal_issue_tracker.shared.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Authentication principal attached to the {@code SecurityContext} by {@link
 * JwtAuthenticationFilter} for every authenticated request. Exposed as a plain class (not a record)
 * so bean-style accessors are guaranteed to resolve in {@code @PreAuthorize} SpEL expressions (e.g.
 * {@code authentication.principal.id}).
 *
 * <p>{@link JsonCreator}/{@link JsonProperty} on the constructor are what let {@code
 * UserAuthenticatedUserLookup}'s {@code @Cacheable} round-trip this through Redis: a {@code final}
 * class with no default constructor is otherwise opaque to {@code GenericJacksonJsonRedisSerializer}
 * on the read side.
 */
public final class AuthenticatedUser {

  private final Integer id;
  private final Role role;

  @JsonCreator
  public AuthenticatedUser(@JsonProperty("id") Integer id, @JsonProperty("role") Role role) {
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
