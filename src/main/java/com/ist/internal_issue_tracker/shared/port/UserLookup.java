package com.ist.internal_issue_tracker.shared.port;

/**
 * Port letting other modules validate a user reference (a team leader, a project leader, an
 * assignee) without depending on the {@code user} module. {@code shared} must never depend on a
 * business module, so the implementation - which needs {@code UserRepository} - lives in {@code
 * user} and is wired in by Spring at runtime.
 *
 * <p>Distinct from {@code shared.security.AuthenticatedUserLookup}, which answers an authentication
 * question about the caller. This one answers a domain question about an arbitrary user id.
 */
public interface UserLookup {

  /**
   * {@code false} if no such user exists <em>or</em> the user is soft-deleted ({@code is_active =
   * false}) - callers should treat both cases the same: an inactive user cannot be handed new
   * responsibilities.
   */
  boolean existsActiveUser(Integer userId);
}
