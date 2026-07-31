package com.ist.internal_issue_tracker.issue.dto;

/**
 * Both fields are optional and independent - see {@code Issue#assigneeTeamId}. Sending only a team
 * hands the work to that team; sending both says a named person on that team has it. Either may be
 * cleared on its own by sending the other alone, and {@code DELETE .../assignee} clears both.
 */
public record ChangeAssigneeRequest(Integer assigneeUserId, Integer assigneeTeamId) {}
