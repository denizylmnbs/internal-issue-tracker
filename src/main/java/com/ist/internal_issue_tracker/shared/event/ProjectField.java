package com.ist.internal_issue_tracker.shared.event;

/**
 * The parts of a project whose change is recorded - see {@link IssueField} for why this is kept
 * apart from the action type the activity log stores.
 *
 * <p>Coarser still than {@link SprintField}: {@code project_activities} has no dates action, so the
 * start and end dates fold into {@code DETAILS} alongside the name and description. Membership is
 * not here at all - adding a person or a team to a project is its own event rather than a field
 * moving, because there is no old value for it to move from.
 */
public enum ProjectField {
  LEADER,
  DETAILS,
  STATUS
}
