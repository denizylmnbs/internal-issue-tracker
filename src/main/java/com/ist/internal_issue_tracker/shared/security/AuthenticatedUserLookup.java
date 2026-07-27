package com.ist.internal_issue_tracker.shared.security;

import java.util.Optional;

/**
 * Port resolved by {@link JwtAuthenticationFilter} to turn a token's userId
 * into an {@link AuthenticatedUser} - roles/authorities are looked up fresh
 * on every request, never trusted from the token itself. {@code shared} must
 * never depend on the {@code user} module, so the implementation (which needs
 * {@code UserRepository}) lives there and is wired in by Spring at runtime;
 * this module only knows the interface.
 */
public interface AuthenticatedUserLookup {

    /** Empty if the user doesn't exist or is no longer active - either way, the request must be treated as unauthenticated. */
    Optional<AuthenticatedUser> findById(Integer userId);
}
