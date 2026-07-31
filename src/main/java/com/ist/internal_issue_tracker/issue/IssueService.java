package com.ist.internal_issue_tracker.issue;

import com.ist.internal_issue_tracker.issue.dto.ChangeAssigneeRequest;
import com.ist.internal_issue_tracker.issue.dto.ChangeStatusRequest;
import com.ist.internal_issue_tracker.issue.dto.IssueCreateRequest;
import com.ist.internal_issue_tracker.issue.dto.IssueResponse;
import com.ist.internal_issue_tracker.issue.dto.IssueUpdateRequest;
import com.ist.internal_issue_tracker.issue.exception.IssueErrorCode;
import com.ist.internal_issue_tracker.issue.exception.IssueNotFoundException;
import com.ist.internal_issue_tracker.issue.mapper.IssueMapper;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.port.EpicLookup;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.port.SprintLookup;
import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * <b>Known limitation.</b> Soft-deleting a sprint or an epic does not clear it from the issues that
 * point at it, so an issue can name a sprint a caller can no longer fetch. Fixing it properly means
 * {@code sprint} reaching into {@code issues} on delete, which is exactly the cross-module write the
 * ports exist to prevent - it belongs to the {@code activity} work, where Modulith events come in.
 * Until then the reference is written once, validated at that moment, and left alone.
 */
@Service
@RequiredArgsConstructor
public class IssueService {

  private final IssueRepository issueRepository;
  private final IssueMapper issueMapper;
  private final ProjectLookup projectLookup;
  private final SprintLookup sprintLookup;
  private final EpicLookup epicLookup;
  private final UserLookup userLookup;
  private final TeamLookup teamLookup;

  private void requireActiveProject(Integer projectId) {
    if (!projectLookup.existsActiveProject(projectId)) {
      throw new AppException(IssueErrorCode.PROJECT_NOT_FOUND);
    }
  }

  /** Both keys - see {@code IssueRepository#findByIdAndProjectIdAndDeletedAtIsNull}. */
  private Issue requireLiveIssue(Integer projectId, Integer issueId) {
    return issueRepository
        .findByIdAndProjectIdAndDeletedAtIsNull(issueId, projectId)
        .orElseThrow(() -> new IssueNotFoundException(issueId));
  }

  /**
   * The sprint and epic are checked against the project, not merely for existence: an issue filed
   * into another project's sprint would show up on that sprint's board while belonging to a project
   * its viewers may not even be on.
   */
  private void requireValidPlacement(Integer projectId, Integer sprintId, Integer epicId) {
    if (sprintId != null && !sprintLookup.existsLiveSprintInProject(projectId, sprintId)) {
      throw new AppException(IssueErrorCode.SPRINT_NOT_FOUND);
    }

    if (epicId != null && !epicLookup.existsLiveEpicInProject(projectId, epicId)) {
      throw new AppException(IssueErrorCode.EPIC_NOT_FOUND);
    }
  }

  /**
   * Assignees only have to exist and be active. Being a participant of the project is deliberately
   * <em>not</em> required - work is sometimes handed to someone outside the project for a day, and
   * refusing that would be a stricter rule than anyone asked for. Tightening it later is one call to
   * {@code ProjectLookup#isParticipantOfProject} in each branch.
   */
  private void requireValidAssignees(Integer assigneeUserId, Integer assigneeTeamId) {
    if (assigneeUserId != null && !userLookup.existsActiveUser(assigneeUserId)) {
      throw new AppException(IssueErrorCode.ASSIGNEE_USER_NOT_FOUND);
    }

    if (assigneeTeamId != null && !teamLookup.existsActiveTeam(assigneeTeamId)) {
      throw new AppException(IssueErrorCode.ASSIGNEE_TEAM_NOT_FOUND);
    }
  }

  /** {@code reporterId} is the caller - see {@code EpicService#createEpic} for why it is trusted. */
  public IssueResponse createIssue(
      Integer projectId, Integer reporterId, IssueCreateRequest request) {
    requireActiveProject(projectId);
    requireValidPlacement(projectId, request.sprintId(), request.epicId());
    requireValidAssignees(request.assigneeUserId(), request.assigneeTeamId());

    Issue issue = issueMapper.toEntity(projectId, reporterId, request);

    return issueMapper.toResponse(issueRepository.save(issue));
  }

  public IssueResponse getIssueById(Integer projectId, Integer issueId) {
    requireActiveProject(projectId);

    return issueMapper.toResponse(requireLiveIssue(projectId, issueId));
  }

  public PagedResponse<IssueResponse> getIssuesByProjectId(
      Integer projectId,
      String name,
      IssueType type,
      IssueStatus status,
      IssuePriority priority,
      Integer sprintId,
      Integer epicId,
      Integer reporterId,
      Integer assigneeUserId,
      Integer assigneeTeamId,
      Pageable pageable) {
    requireActiveProject(projectId);

    // derived query, so the caller's sort is honoured as-is
    Page<Issue> issues =
        issueRepository.findAllByFilters(
            projectId,
            name,
            type,
            status,
            priority,
            sprintId,
            epicId,
            reporterId,
            assigneeUserId,
            assigneeTeamId,
            pageable);

    return PagedResponse.from(issues.map(issueMapper::toResponse));
  }

  public IssueResponse updateIssue(
      Integer projectId, Integer issueId, IssueUpdateRequest request) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    requireValidPlacement(projectId, request.sprintId(), request.epicId());

    issueMapper.updateEntity(issue, request);

    return issueMapper.toResponse(issueRepository.save(issue));
  }

  /**
   * Any status may follow any other, and no database constraint stands behind this one either -
   * {@code issues} carries nothing like the sprint table's "one in progress per project" index.
   */
  public IssueResponse changeStatus(
      Integer projectId, Integer issueId, ChangeStatusRequest request) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    issue.setStatus(request.status());

    return issueMapper.toResponse(issueRepository.save(issue));
  }

  /**
   * Sets both assignee fields to what was sent, so passing one alone clears the other. That is the
   * point of it being a replacement of the assignment rather than a patch of it: "this is now the
   * team's, nobody in particular" has to be expressible.
   */
  public IssueResponse changeAssignee(
      Integer projectId, Integer issueId, ChangeAssigneeRequest request) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    requireValidAssignees(request.assigneeUserId(), request.assigneeTeamId());

    issue.setAssigneeUserId(request.assigneeUserId());
    issue.setAssigneeTeamId(request.assigneeTeamId());

    return issueMapper.toResponse(issueRepository.save(issue));
  }

  /** Leaves the issue unassigned entirely - the counterpart of {@code ProjectService#removeLeader}. */
  public IssueResponse removeAssignee(Integer projectId, Integer issueId) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    issue.setAssigneeUserId(null);
    issue.setAssigneeTeamId(null);

    return issueMapper.toResponse(issueRepository.save(issue));
  }

  /** Soft delete: the row stays and {@code deletedAt} is stamped; the status is left where it was. */
  public void deleteIssue(Integer projectId, Integer issueId) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    issue.setDeletedAt(OffsetDateTime.now());

    issueRepository.save(issue);
  }
}
