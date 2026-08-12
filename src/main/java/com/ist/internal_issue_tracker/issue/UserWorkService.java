package com.ist.internal_issue_tracker.issue;

import com.ist.internal_issue_tracker.issue.dto.IssueResponse;
import com.ist.internal_issue_tracker.issue.dto.UserSprintProgressResponse;
import com.ist.internal_issue_tracker.issue.dto.UserSprintProgressResponse.SprintProgress;
import com.ist.internal_issue_tracker.issue.mapper.IssueMapper;
import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;
import com.ist.internal_issue_tracker.shared.port.FieldDefinitionLookup;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import com.ist.internal_issue_tracker.shared.port.FieldSemantic;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.port.SprintLookup;
import com.ist.internal_issue_tracker.shared.port.SprintSummary;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * "My Work"'s two questions about one person: what are they carrying right now, and how has that
 * gone lately. Neither is a project concern the way {@code IssueService} or {@code
 * IssueMetricsService} are - both cut across every project the person touches - so this stands on
 * its own rather than extending either.
 *
 * <p>Both queries used to run as a single {@code status IN (:set)} / {@code GROUP BY ... CASE WHEN}
 * query against a fixed, global {@code IssueStatus} enum. That is no longer possible: statuses are
 * project-scoped data now, so "done", "cancelled" and "in flight" can each mean a different set of
 * codes on every project a person touches, and this module cannot join {@code field_definitions} to
 * resolve that in SQL (see {@code fielddef/package-info.java}). Both methods below fetch the raw,
 * live rows for one person - unpaged, since a person's own open work is a bounded set - and
 * classify each one against its own project's semantics via {@link FieldDefinitionLookup}.
 *
 * <p>One deliberate narrowing versus the old fixed set: "active" used to mean {@code TODO},
 * {@code IN_PROGRESS} or {@code IN_REVIEW} specifically (excluding {@code BACKLOG} and {@code
 * ON_HOLD} as well as the two closed states). The field-definition flags this migration introduces
 * only distinguish done/cancelled/active-work/defect, not "backlog" or "on hold" as their own
 * concepts, so "active" here is approximated as "not done and not cancelled". A team that wants
 * on-hold work excluded from "My Work" has to say so with a status of its own for now.
 */
@Service
@RequiredArgsConstructor
public class UserWorkService {

  /** How many of the most recently finished sprints feed the average - see the class javadoc. */
  private static final int RECENT_SPRINT_WINDOW = 6;

  private final IssueRepository issueRepository;
  private final IssueMapper issueMapper;
  private final UserLookup userLookup;
  private final ProjectLookup projectLookup;
  private final SprintLookup sprintLookup;
  private final FieldDefinitionLookup fieldDefinitionLookup;

  private void requireActiveUser(Integer userId) {
    if (!userLookup.existsActiveUser(userId)) {
      throw ResourceNotFoundException.of("User", userId);
    }
  }

  /** Every closed (done or cancelled) status code this project currently has, in one lookup. */
  private Set<String> closedCodes(Integer projectId) {
    Set<String> closed = new java.util.HashSet<>();
    closed.addAll(
        fieldDefinitionLookup.codesWithSemantic(projectId, FieldKind.ISSUE_STATUS, FieldSemantic.DONE));
    closed.addAll(
        fieldDefinitionLookup.codesWithSemantic(
            projectId, FieldKind.ISSUE_STATUS, FieldSemantic.CANCELLED));
    return closed;
  }

  private boolean isDone(Integer projectId, String status, Map<Integer, Set<String>> doneCache) {
    Set<String> done =
        doneCache.computeIfAbsent(
            projectId,
            id ->
                fieldDefinitionLookup.codesWithSemantic(id, FieldKind.ISSUE_STATUS, FieldSemantic.DONE));
    return done.contains(status);
  }

  /**
   * Manual pagination over an in-memory list - unavoidable once the filter itself (per-project
   * closed-status sets) can no longer be expressed as a single SQL predicate. See the class javadoc.
   */
  private static <T> Page<T> paginate(List<T> all, Pageable pageable) {
    int start = (int) pageable.getOffset();
    if (start >= all.size()) {
      return new PageImpl<>(List.of(), pageable, all.size());
    }
    int end = Math.min(start + pageable.getPageSize(), all.size());
    return new PageImpl<>(all.subList(start, end), pageable, all.size());
  }

  public PagedResponse<IssueResponse> getActiveIssuesByUserId(Integer userId, Pageable pageable) {
    requireActiveUser(userId);

    List<Issue> allAssigned = issueRepository.findByAssigneeUserIdAndDeletedAtIsNull(userId);

    Map<Integer, Set<String>> closedCache = new HashMap<>();
    List<Issue> active =
        allAssigned.stream()
            .filter(
                issue ->
                    !closedCache
                        .computeIfAbsent(issue.getProjectId(), this::closedCodes)
                        .contains(issue.getStatus()))
            .sorted(Comparator.comparing(Issue::getUpdatedAt).reversed())
            .toList();

    Page<Issue> page = paginate(active, pageable);

    return PagedResponse.from(page.map(issueMapper::toResponse));
  }

  public UserSprintProgressResponse getSprintProgress(Integer userId) {
    requireActiveUser(userId);

    List<Issue> rows =
        issueRepository.findByAssigneeUserIdAndDeletedAtIsNullAndSprintIdIsNotNull(userId);

    Map<Integer, Set<String>> cancelledCache = new HashMap<>();
    Map<Integer, Set<String>> doneCache = new HashMap<>();

    // one entry per (projectId, sprintId), same grouping the old GROUP BY produced
    Map<List<Integer>, List<Issue>> bySprint =
        rows.stream()
            .filter(
                issue ->
                    !cancelledCache
                        .computeIfAbsent(
                            issue.getProjectId(),
                            id ->
                                fieldDefinitionLookup.codesWithSemantic(
                                    id, FieldKind.ISSUE_STATUS, FieldSemantic.CANCELLED))
                        .contains(issue.getStatus()))
            .collect(Collectors.groupingBy(i -> List.of(i.getProjectId(), i.getSprintId())));

    List<UserSprintPointsRow> points = new ArrayList<>();
    for (Map.Entry<List<Integer>, List<Issue>> entry : bySprint.entrySet()) {
      Integer projectId = entry.getKey().get(0);
      Integer sprintId = entry.getKey().get(1);
      List<Issue> issues = entry.getValue();

      long assignedPoints = issues.stream().mapToLong(i -> pointsOf(i)).sum();
      long completedPoints =
          issues.stream()
              .filter(i -> isDone(projectId, i.getStatus(), doneCache))
              .mapToLong(i -> pointsOf(i))
              .sum();
      long completedIssueCount =
          issues.stream().filter(i -> isDone(projectId, i.getStatus(), doneCache)).count();

      points.add(
          new UserSprintPointsRow(
              projectId, sprintId, assignedPoints, completedPoints, issues.size(), completedIssueCount));
    }

    Set<Integer> projectIds =
        points.stream().map(UserSprintPointsRow::projectId).collect(Collectors.toSet());

    Map<Integer, SprintSummary> summariesBySprintId =
        projectIds.stream()
            .filter(projectLookup::existsActiveProject)
            .flatMap(projectId -> sprintLookup.findSprintSummaries(projectId).stream())
            .collect(Collectors.toMap(SprintSummary::id, summary -> summary, (a, b) -> a));

    List<SprintProgress> current = toSprintProgress(points, summariesBySprintId, true);
    List<SprintProgress> everyCompleted = toSprintProgress(points, summariesBySprintId, false);

    List<SprintProgress> previous =
        everyCompleted.stream()
            // one entry per project: the most recently finished sprint, not every finished one
            .collect(Collectors.groupingBy(SprintProgress::projectId))
            .values()
            .stream()
            .map(
                perProject ->
                    perProject.stream().max(Comparator.comparing(SprintProgress::endDate)).orElseThrow())
            .toList();

    List<SprintProgress> recentCompleted =
        everyCompleted.stream()
            .sorted(Comparator.comparing(SprintProgress::endDate).reversed())
            .limit(RECENT_SPRINT_WINDOW)
            .toList();

    Double recentAveragePoints =
        recentCompleted.isEmpty()
            ? null
            : recentCompleted.stream().mapToLong(SprintProgress::completedPoints).average().orElseThrow();

    return new UserSprintProgressResponse(
        current, previous, recentAveragePoints, recentCompleted.size());
  }

  private static long pointsOf(Issue issue) {
    return issue.getStoryPoint() == null ? 0L : issue.getStoryPoint();
  }

  /**
   * {@code wantRunning} selects between a sprint carrying this project's {@code isActiveWork}
   * status (the "current" bucket, replacing the old literal {@code SPRINT_STATUS = 'IN_PROGRESS'}
   * check) and one carrying {@code isDone} (the "completed" bucket, replacing {@code 'COMPLETED'}).
   */
  private List<SprintProgress> toSprintProgress(
      List<UserSprintPointsRow> rows,
      Map<Integer, SprintSummary> summariesBySprintId,
      boolean wantRunning) {
    Map<Integer, Set<String>> runningCache = new HashMap<>();
    Map<Integer, Set<String>> doneCache = new HashMap<>();

    return rows.stream()
        .filter(
            row -> {
              SprintSummary summary = summariesBySprintId.get(row.sprintId());
              if (summary == null || summary.status() == null) {
                return false;
              }
              Set<String> target =
                  wantRunning
                      ? runningCache.computeIfAbsent(
                          row.projectId(),
                          id ->
                              fieldDefinitionLookup.codesWithSemantic(
                                  id, FieldKind.SPRINT_STATUS, FieldSemantic.ACTIVE_WORK))
                      : doneCache.computeIfAbsent(
                          row.projectId(),
                          id ->
                              fieldDefinitionLookup.codesWithSemantic(
                                  id, FieldKind.SPRINT_STATUS, FieldSemantic.DONE));
              return target.contains(summary.status());
            })
        .map(row -> toSprintProgress(row, summariesBySprintId.get(row.sprintId())))
        .toList();
  }

  private static SprintProgress toSprintProgress(UserSprintPointsRow row, SprintSummary summary) {
    return new SprintProgress(
        row.projectId(),
        row.sprintId(),
        summary.name(),
        summary.startDate(),
        summary.endDate(),
        row.assignedPoints(),
        row.completedPoints(),
        row.assignedIssueCount(),
        row.completedIssueCount());
  }

  /** In-memory replacement for the old {@code UserSprintPoints} JPA projection - see class javadoc. */
  private record UserSprintPointsRow(
      Integer projectId,
      Integer sprintId,
      long assignedPoints,
      long completedPoints,
      long assignedIssueCount,
      long completedIssueCount) {}
}
