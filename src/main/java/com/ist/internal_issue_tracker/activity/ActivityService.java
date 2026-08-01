package com.ist.internal_issue_tracker.activity;

import com.ist.internal_issue_tracker.activity.dto.ActivityResponse;
import com.ist.internal_issue_tracker.activity.exception.ActivityErrorCode;
import com.ist.internal_issue_tracker.activity.mapper.ActivityMapper;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.port.IssueLookup;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.port.SprintLookup;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Reads history back. There is no write path here at all - rows arrive through the listeners and
 * nothing else may put one in, which is what makes this a record rather than a table people edit.
 *
 * <p>Everything is addressed under a project, and the parent is checked through a {@code shared}
 * port before the history is read. Without that check the endpoints would be an oracle: asking for
 * another project's issue would return either rows or an empty page, and the difference would tell
 * the caller whether that issue exists.
 */
@Service
@RequiredArgsConstructor
public class ActivityService {

  /**
   * The caller's sort is deliberately dropped in favour of newest-first.
   *
   * <p>A history read in an arbitrary order is not a history, and the id tie-break is not decoration:
   * one operation writes several rows carrying the same instant - a rename, a re-prioritisation and a
   * re-estimate from a single update - so ordering on the timestamp alone leaves them in whatever
   * order the database happens to return.
   */
  private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

  private final IssueActivityRepository issueActivityRepository;
  private final SprintActivityRepository sprintActivityRepository;
  private final ProjectActivityRepository projectActivityRepository;
  private final ActivityMapper activityMapper;
  private final ProjectLookup projectLookup;
  private final IssueLookup issueLookup;
  private final SprintLookup sprintLookup;

  private void requireActiveProject(Integer projectId) {
    if (!projectLookup.existsActiveProject(projectId)) {
      throw new AppException(ActivityErrorCode.PROJECT_NOT_FOUND);
    }
  }

  private static Pageable newestFirst(Pageable pageable) {
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
  }

  /**
   * The issue is checked against the project rather than merely for existence, so that history
   * cannot be read through a project the caller happens to be authorized on.
   */
  public PagedResponse<ActivityResponse> getIssueActivities(
      Integer projectId, Integer issueId, Pageable pageable) {
    requireActiveProject(projectId);

    if (!issueLookup.existsLiveIssueInProject(projectId, issueId)) {
      throw new AppException(ActivityErrorCode.ISSUE_NOT_FOUND);
    }

    return PagedResponse.from(
        issueActivityRepository
            .findAllByIssueId(issueId, newestFirst(pageable))
            .map(activityMapper::toResponse));
  }

  public PagedResponse<ActivityResponse> getSprintActivities(
      Integer projectId, Integer sprintId, Pageable pageable) {
    requireActiveProject(projectId);

    if (!sprintLookup.existsLiveSprintInProject(projectId, sprintId)) {
      throw new AppException(ActivityErrorCode.SPRINT_NOT_FOUND);
    }

    return PagedResponse.from(
        sprintActivityRepository
            .findAllBySprintId(sprintId, newestFirst(pageable))
            .map(activityMapper::toResponse));
  }

  /**
   * The project's own history - who led it, when it changed status, who was put on it. Not the
   * issues' history, which is read per issue: merging the three tables into one feed would mean a
   * union across them, and the three answer different questions.
   */
  public PagedResponse<ActivityResponse> getProjectActivities(Integer projectId, Pageable pageable) {
    requireActiveProject(projectId);

    return PagedResponse.from(
        projectActivityRepository
            .findAllByProjectId(projectId, newestFirst(pageable))
            .map(activityMapper::toResponse));
  }
}
