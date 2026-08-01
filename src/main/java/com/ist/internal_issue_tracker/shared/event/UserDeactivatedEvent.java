package com.ist.internal_issue_tracker.shared.event;

/**
 * Published when a user is soft-deleted. Every module holding rows that point at a user is expected
 * to retire them in response, so that "is this person still around" is answered by the row itself
 * rather than by joining back to {@code users} on every read.
 *
 * <p>Lives in {@code shared} for the same reason the lookup ports do: {@code team} and {@code
 * project} must be able to react without depending on the {@code user} module.
 *
 * <p>Delivered synchronously, inside the publisher's transaction. That is the point - a deactivated
 * user must never be visible on a roster, and an asynchronous listener would leave a window where
 * they are.
 */
public record UserDeactivatedEvent(Integer userId) {}
