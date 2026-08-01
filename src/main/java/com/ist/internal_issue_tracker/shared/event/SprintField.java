package com.ist.internal_issue_tracker.shared.event;

/**
 * The parts of a sprint whose change is recorded - the sprint counterpart of {@link IssueField}, and
 * kept separate from the action type stored against {@code sprint_activities} for the same reason.
 *
 * <p>Coarser than the issue set because the table is: the two dates share one action, as do the name
 * and the description, so moving a start date and an end date together is one change rather than
 * two.
 */
public enum SprintField {
  STATUS,
  DATES,
  DETAILS
}
