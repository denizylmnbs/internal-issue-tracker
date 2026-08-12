package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;

/**
 * A user joining or leaving a team - the mirror of {@link ProjectMembershipEvent}'s {@code USER}
 * case, one level down. Exists so a module outside {@code team} (namely {@code project}'s
 * participant cache) can react to a roster change without reading {@code team_users} itself.
 *
 * <p>No {@code actorId}: unlike {@code project}, {@code team} does not thread the caller through
 * its membership endpoints today, and this event has no audit use that would need one - only cache
 * invalidation, which cares who the change is about, not who made it.
 */
public record TeamMembershipEvent(
    Integer teamId, Integer userId, Change change, OffsetDateTime occurredAt) {

  public enum Change {
    ADDED,
    REMOVED
  }
}
