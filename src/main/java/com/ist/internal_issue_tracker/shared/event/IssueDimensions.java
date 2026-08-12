package com.ist.internal_issue_tracker.shared.event;

/**
 * What was true of the issue at the moment the event happened, for the metrics to slice by.
 *
 * <p>Carried on every issue event rather than looked up by the consumer, and the difference is not
 * an optimisation. A lookup would answer with the issue's state <em>now</em>, so re-pointing an
 * issue from 3 to 8 today would rewrite the velocity of the sprint it shipped in last quarter, and
 * moving an issue to the next sprint would erase it from the previous sprint's burndown as though
 * it had never been committed. The number would change without the history changing, which is the
 * one thing an audit log exists to prevent.
 *
 * <p>These are therefore <em>as-of</em> values: the issue's state after the change this event
 * describes, frozen. They land in the columns {@code V3} added to {@code issue_activities}.
 *
 * <p>{@code type} and {@code priority} are strings rather than enums because {@code shared} cannot
 * name {@code issue.IssueType} - the same reason {@link IssueFieldChange} renders its values as
 * text. The publisher writes {@code enum.name()} and the storage side's CHECK constraint is what
 * catches a value that has drifted.
 *
 * <p>Every field is nullable, and each null means the issue genuinely had nothing there: no type
 * set, no estimate agreed, no sprint yet. {@code priority} is the exception in practice - {@code
 * issues} declares it {@code NOT NULL} with a default - but it is left nullable here so that the
 * record does not encode a constraint belonging to another module.
 *
 * <p>Notably absent: the assignee. It would fit, and per-person throughput would fall straight out
 * of it - which is exactly why it is not here. See {@code IssueMetricsController} for why these
 * numbers stop at the project.
 */
public record IssueDimensions(String type, String priority, Integer storyPoint, Integer sprintId) {}
