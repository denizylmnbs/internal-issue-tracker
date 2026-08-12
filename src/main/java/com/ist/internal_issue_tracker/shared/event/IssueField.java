package com.ist.internal_issue_tracker.shared.event;

/**
 * The parts of an issue whose change is worth recording, named in the vocabulary of the module that
 * publishes the change rather than of the one that stores it.
 *
 * <p>This is deliberately not the {@code action_type} the activity log writes. That enum mirrors a
 * database {@code CHECK} constraint and belongs to whoever owns the table; this one is the contract
 * {@code issue} speaks. The two are mapped in one place, in the listener, so a new column on the
 * audit side does not oblige {@code issue} to learn about it - and so a rename on either side fails
 * to compile instead of silently writing the wrong row.
 *
 * <p>A plain {@code String} field name would do the same job in fewer lines and is exactly what
 * this avoids: a typo in a string reaches production as a missing metric rather than as a build
 * error.
 *
 * <p>There is no constant for an issue's epic. The {@code issue_activities} CHECK has no {@code
 * EPIC_UPDATED} action, so a change to it has nowhere to be written; adding one here without the
 * migration agreeing would only fail at insert time. If that history is wanted, the constraint has
 * to be widened first.
 */
public enum IssueField {
  STATUS,
  PRIORITY,
  ASSIGNEE_USER,
  ASSIGNEE_TEAM,
  SPRINT,
  STORY_POINT,
  DETAILS
}
