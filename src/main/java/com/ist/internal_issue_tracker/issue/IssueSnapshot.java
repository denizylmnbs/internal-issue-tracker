package com.ist.internal_issue_tracker.issue;

/**
 * The audited fields of an issue, copied off it before a write so they can be compared with what
 * the write left behind.
 *
 * <p>A copy is necessary rather than fussy: the entity {@code IssueService} mutates is the managed
 * one, so reading a field after the update returns the new value however early it is read. Taking
 * the snapshot is the only moment the old values still exist.
 *
 * <p>It covers every field the activity log has an action type for, not merely the ones a given
 * endpoint is expected to touch, so that one {@code diff} serves all of them and no write path can
 * forget a field. {@code epicId} is absent for the opposite reason - {@code issue_activities} has
 * no action type for it, so a change there has nowhere to be recorded.
 */
record IssueSnapshot(
    String name,
    String description,
    IssueType type,
    IssueStatus status,
    IssuePriority priority,
    Integer storyPoint,
    Integer sprintId,
    Integer assigneeUserId,
    Integer assigneeTeamId) {

  static IssueSnapshot of(Issue issue) {
    return new IssueSnapshot(
        issue.getName(),
        issue.getDescription(),
        issue.getType(),
        issue.getStatus(),
        issue.getPriority(),
        issue.getStoryPoint(),
        issue.getSprintId(),
        issue.getAssigneeUserId(),
        issue.getAssigneeTeamId());
  }
}
