package com.ist.internal_issue_tracker.issue.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * One person's sprint progress across every project they hold issues in - the numbers behind "My
 * Work"'s progress tiles.
 *
 * <p>{@code current} and {@code previous} are lists, not single entries, because a person can carry
 * work in more than one project at once and each project runs its own sprint independently. A
 * caller wanting one headline number sums the list itself; the breakdown is kept so the UI can
 * still show which project a number came from.
 *
 * <p>{@code recentAveragePoints} is null when the person has not finished a single sprint yet - see
 * {@code UserWorkService#getSprintProgress}. Null there means "no answer", not "zero"; a caller
 * that coalesces it would report a newcomer as averaging nothing, which is a different claim.
 */
public record UserSprintProgressResponse(
    List<SprintProgress> current,
    List<SprintProgress> previous,
    Double recentAveragePoints,
    int recentSprintCount) {

  public record SprintProgress(
      Integer projectId,
      Integer sprintId,
      String sprintName,
      LocalDate startDate,
      LocalDate endDate,
      long assignedPoints,
      long completedPoints,
      long assignedIssueCount,
      long completedIssueCount) {}
}
