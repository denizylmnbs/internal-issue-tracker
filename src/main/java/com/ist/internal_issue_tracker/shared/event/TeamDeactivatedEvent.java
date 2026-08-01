package com.ist.internal_issue_tracker.shared.event;

/**
 * Published when a team is soft-deleted. Its roster in {@code team} and its assignments in {@code
 * project} are retired in response - see {@link UserDeactivatedEvent} for the delivery contract.
 */
public record TeamDeactivatedEvent(Integer teamId) {}
