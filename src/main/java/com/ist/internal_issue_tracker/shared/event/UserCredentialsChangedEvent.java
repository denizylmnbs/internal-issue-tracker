package com.ist.internal_issue_tracker.shared.event;

/**
 * Published whenever a user's password is changed or reset. {@code auth} listens for this to revoke
 * every refresh token the user currently holds, so a session issued before the change cannot keep
 * renewing itself past it - the same reasoning {@link UserDeactivatedEvent} follows for
 * deactivation, just for the case where the account stays active but the credential itself moved.
 *
 * <p>Lives in {@code shared} for the same reason {@link UserDeactivatedEvent} does: {@code auth}
 * must be able to react without depending on the {@code user} module.
 */
public record UserCredentialsChangedEvent(Integer userId) {}
