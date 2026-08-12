package com.ist.internal_issue_tracker.issue;

import com.ist.internal_issue_tracker.issue.dto.IssueResponse;
import com.ist.internal_issue_tracker.issue.dto.UserSprintProgressResponse;
import com.ist.internal_issue_tracker.issue.dto.UserSprintProgressResponse.SprintProgress;
import com.ist.internal_issue_tracker.issue.mapper.IssueMapper;
import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.port.SprintLookup;
import com.ist.internal_issue_tracker.shared.port.SprintSummary;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * "My Work"'s two questions about one person: what are they carrying right now, and how has that
 * gone lately. Neither is a project concern the way {@code IssueService} or {@code
 * IssueMetricsService} are - both cut across every project the person touches - so this stands on
 * its own rather than extending either.
 */
@Service
@RequiredArgsConstructor
public class UserWorkService {

  /** BACKLOG is unplanned, ON_HOLD is parked, DONE/CANCELLED are closed - none is "in flight". */
  private static final Set<IssueStatus> ACTIVE_STATUSES =
      EnumSet.of(IssueStatus.TODO, IssueStatus.IN_PROGRESS, IssueStatus.IN_REVIEW);

  private static final String SPRINT_STATUS_IN_PROGRESS = "IN_PROGRESS";
  private static final String SPRINT_STATUS_COMPLETED = "COMPLETED";

  /** How many of the most recently finished sprints feed the average - see the class javadoc. */
  private static final int RECENT_SPRINT_WINDOW = 6;

  private final IssueRepository issueRepository;
  private final IssueMapper issueMapper;
  private final UserLookup userLookup;
  private final ProjectLookup projectLookup;
  private final SprintLookup sprintLookup;

  /**
   * Rows whose sprint has since been soft-deleted are dropped, the same way {@code
   * IssueMetricsService#velocity} treats an activity row with no surviving sprint - an orphaned id
   * has nothing to report a name or date for.
   */
  private static List<SprintProgress> toSprintProgress(
      List<UserSprintPoints> rows, Map<Integer, SprintSummary> summariesBySprintId, String status) {
    return rows.stream()
        .filter(row -> status.equals(summaryStatus(summariesBySprintId, row.getSprintId())))
        .map(row -> toSprintProgress(row, summariesBySprintId.get(row.getSprintId())))
        .toList();
  }

  private static String summaryStatus(Map<Integer, SprintSummary> summaries, Integer sprintId) {
    SprintSummary summary = summaries.get(sprintId);
    return summary == null ? null : summary.status();
  }

  private static SprintProgress toSprintProgress(UserSprintPoints row, SprintSummary summary) {
    return new SprintProgress(
        row.getProjectId(),
        row.getSprintId(),
        summary.name(),
        summary.startDate(),
        summary.endDate(),
        row.getAssignedPoints(),
        row.getCompletedPoints(),
        row.getAssignedIssueCount(),
        row.getCompletedIssueCount());
  }

  private void requireActiveUser(Integer userId) {
    if (!userLookup.existsActiveUser(userId)) {
      throw ResourceNotFoundException.of("User", userId);
    }
  }

  public PagedResponse<IssueResponse> getActiveIssuesByUserId(Integer userId, Pageable pageable) {
    requireActiveUser(userId);

    Page<Issue> issues =
        issueRepository.findByAssigneeAndStatuses(userId, ACTIVE_STATUSES, pageable);

    return PagedResponse.from(issues.map(issueMapper::toResponse));
  }

  public UserSprintProgressResponse getSprintProgress(Integer userId) {
    requireActiveUser(userId);

    List<UserSprintPoints> rows =
        issueRepository.sprintPointsByAssignee(userId, IssueStatus.DONE, IssueStatus.CANCELLED);

    Set<Integer> projectIds =
        rows.stream().map(UserSprintPoints::getProjectId).collect(Collectors.toSet());

    Map<Integer, SprintSummary> summariesBySprintId =
        projectIds.stream()
            .filter(projectLookup::existsActiveProject)
            .flatMap(projectId -> sprintLookup.findSprintSummaries(projectId).stream())
            .collect(Collectors.toMap(SprintSummary::id, summary -> summary, (a, b) -> a));

    List<SprintProgress> current =
        toSprintProgress(rows, summariesBySprintId, SPRINT_STATUS_IN_PROGRESS);
    List<SprintProgress> previous =
        toSprintProgress(rows, summariesBySprintId, SPRINT_STATUS_COMPLETED).stream()
            // one entry per project: the most recently finished sprint, not every finished one
            .collect(Collectors.groupingBy(SprintProgress::projectId))
            .values()
            .stream()
            .map(
                perProject ->
                    perProject.stream()
                        .max(Comparator.comparing(SprintProgress::endDate))
                        .orElseThrow())
            .toList();

    List<SprintProgress> recentCompleted =
        toSprintProgress(rows, summariesBySprintId, SPRINT_STATUS_COMPLETED).stream()
            .sorted(Comparator.comparing(SprintProgress::endDate).reversed())
            .limit(RECENT_SPRINT_WINDOW)
            .toList();

    Double recentAveragePoints =
        recentCompleted.isEmpty()
            ? null
            : recentCompleted.stream()
                .mapToLong(SprintProgress::completedPoints)
                .average()
                .orElseThrow();

    return new UserSprintProgressResponse(
        current, previous, recentAveragePoints, recentCompleted.size());
  }
}
