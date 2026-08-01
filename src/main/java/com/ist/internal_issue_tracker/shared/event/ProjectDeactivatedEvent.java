package com.ist.internal_issue_tracker.shared.event;

/**
 * Published when a project is soft-deleted. Its direct member rows and team assignments are retired
 * in response - see {@link UserDeactivatedEvent} for the delivery contract.
 *
 * <p>Both listeners live in {@code project} alongside the publisher; the event exists rather than a
 * direct call so the cleanup stays uniform with the other two and one more module reacting later
 * costs nothing here.
 */
public record ProjectDeactivatedEvent(Integer projectId) {}
