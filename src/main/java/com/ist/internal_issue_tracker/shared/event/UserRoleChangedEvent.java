package com.ist.internal_issue_tracker.shared.event;

/**
 * Published when a user's role changes. Two independent things depend on this firing: the {@code
 * auth-principal} Redis cache entry {@code UserAuthenticatedUserLookup} resolves the user's
 * authorities from must be evicted so the new role takes effect on the very next request instead of
 * waiting out the cache TTL, and any refresh token issued under the old role must stop working so a
 * session started before the change cannot keep renewing itself past it.
 *
 * <p>Lives in {@code shared} for the same reason {@link UserDeactivatedEvent} does: listeners in
 * other modules (here, {@code auth}) react without depending on {@code user}.
 *
 * <p>Delivered synchronously, inside the publisher's transaction - the same reasoning as {@link
 * UserDeactivatedEvent}: an async gap would leave the old role's cached grant, or its refresh tokens,
 * usable for however long delivery takes.
 */
public record UserRoleChangedEvent(Integer userId) {}
