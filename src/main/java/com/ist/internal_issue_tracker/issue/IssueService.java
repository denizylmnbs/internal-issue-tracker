package com.ist.internal_issue_tracker.issue;

import com.ist.internal_issue_tracker.issue.dto.ChangeAssigneeRequest;
import com.ist.internal_issue_tracker.issue.dto.ChangeStatusRequest;
import com.ist.internal_issue_tracker.issue.dto.IssueCreateRequest;
import com.ist.internal_issue_tracker.issue.dto.IssueResponse;
import com.ist.internal_issue_tracker.issue.dto.IssueUpdateRequest;
import com.ist.internal_issue_tracker.issue.exception.IssueErrorCode;
import com.ist.internal_issue_tracker.issue.exception.IssueNotFoundException;
import com.ist.internal_issue_tracker.issue.mapper.IssueMapper;
import com.ist.internal_issue_tracker.shared.event.IssueChangedEvent;
import com.ist.internal_issue_tracker.shared.event.IssueCreatedEvent;
import com.ist.internal_issue_tracker.shared.event.IssueDeletedEvent;
import com.ist.internal_issue_tracker.shared.event.IssueFieldChange;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.port.EpicLookup;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.port.SprintLookup;
import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Known limitation.</b> Soft-deleting a sprint or an epic does not clear it from the issues that
 * point at it, so an issue can name a sprint a caller can no longer fetch. Fixing it properly means
 * {@code sprint} reaching into {@code issues} on delete, which is exactly the cross-module write the
 * ports exist to prevent - it belongs to the {@code activity} work, where Modulith events come in.
 * Until then the reference is written once, validated at that moment, and left alone.
 *
 * <p><b>On {@code actorId}.</b> Every write takes the id of whoever is making the change, separately
 * from the reporter who filed the issue and the assignee it belongs to. Those two are properties of
 * the issue; the actor is a property of the <em>change</em>, and no column on {@code issues} holds
 * it - a lead moving someone else's issue to {@code DONE} leaves no trace of themselves today. It is
 * what {@code issue_activities.user_id} is written from. It always comes from the authenticated
 * principal and never from a request body, for the reason given on {@code
 * IssueController#createIssue}.
 *
 * <p><b>Why every write is {@code @Transactional}.</b> The activity log is fed by events consumed
 * after commit. Spring only delivers those to a listener if there was a transaction to commit, and
 * {@code repository.save()} opening and closing its own is not one this method knows about - so
 * without the annotation the event is dropped with no exception, no log and no publication row,
 * while the write itself succeeds. The failure is invisible until someone asks why the metrics are
 * empty. It is not a cost: these methods already issue two or three statements, each in its own
 * transaction today, and this collapses them into one.
 */
@Service
@RequiredArgsConstructor
public class IssueService {

  private final IssueRepository issueRepository;
  private final IssueMapper issueMapper;
  private final IssueChangeDetector issueChangeDetector;
  private final ProjectLookup projectLookup;
  private final SprintLookup sprintLookup;
  private final EpicLookup epicLookup;
  private final UserLookup userLookup;
  private final TeamLookup teamLookup;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Diffs the issue against the snapshot and publishes only if something moved - see {@code
   * IssueChangeDetector}. The clock is read here rather than in the listener, once per operation, so
   * that every row from one change carries the moment the change happened.
   */
  private void publishChanges(Integer projectId, Integer actorId, IssueSnapshot before, Issue after) {
    List<IssueFieldChange> changes = issueChangeDetector.diff(before, after);

    if (changes.isEmpty()) {
      return;
    }

    eventPublisher.publishEvent(
        new IssueChangedEvent(
            after.getId(), projectId, actorId, OffsetDateTime.now(), changes));
  }

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

  /**
   * {@code reporterId} is the caller - see {@code EpicService#createEpic} for why it is trusted. It
   * is also the actor here, the one moment the two coincide.
   */
  @Transactional
  public IssueResponse createIssue(
      Integer projectId, Integer reporterId, IssueCreateRequest request) {
    requireActiveProject(projectId);
    requireValidPlacement(projectId, request.sprintId(), request.epicId());
    requireValidAssignees(request.assigneeUserId(), request.assigneeTeamId());

    Issue issue = issueMapper.toEntity(projectId, reporterId, request);

    Issue savedIssue = issueRepository.save(issue);

    eventPublisher.publishEvent(
        new IssueCreatedEvent(savedIssue.getId(), projectId, reporterId, OffsetDateTime.now()));

    return issueMapper.toResponse(savedIssue);
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

  /**
   * The snapshot is taken before {@code updateEntity} because that mutates the managed entity - once
   * it has run there is nothing left to compare against.
   */
  @Transactional
  public IssueResponse updateIssue(
      Integer projectId, Integer issueId, Integer actorId, IssueUpdateRequest request) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    requireValidPlacement(projectId, request.sprintId(), request.epicId());

    IssueSnapshot before = IssueSnapshot.of(issue);

    issueMapper.updateEntity(issue, request);

    Issue savedIssue = issueRepository.save(issue);

    publishChanges(projectId, actorId, before, savedIssue);

    return issueMapper.toResponse(savedIssue);
  }

  /**
   * Any status may follow any other, and no database constraint stands behind this one either -
   * {@code issues} carries nothing like the sprint table's "one in progress per project" index.
   */
  @Transactional
  public IssueResponse changeStatus(
      Integer projectId, Integer issueId, Integer actorId, ChangeStatusRequest request) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setStatus(request.status());

    Issue savedIssue = issueRepository.save(issue);

    // every cycle time, lead time and throughput number is built from the rows this line writes
    publishChanges(projectId, actorId, before, savedIssue);

    return issueMapper.toResponse(savedIssue);
  }

  /**
   * Sets both assignee fields to what was sent, so passing one alone clears the other. That is the
   * point of it being a replacement of the assignment rather than a patch of it: "this is now the
   * team's, nobody in particular" has to be expressible.
   */
  @Transactional
  public IssueResponse changeAssignee(
      Integer projectId, Integer issueId, Integer actorId, ChangeAssigneeRequest request) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    requireValidAssignees(request.assigneeUserId(), request.assigneeTeamId());

    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setAssigneeUserId(request.assigneeUserId());
    issue.setAssigneeTeamId(request.assigneeTeamId());

    Issue savedIssue = issueRepository.save(issue);

    // one row, or two, or none - whichever of the pair actually moved
    publishChanges(projectId, actorId, before, savedIssue);

    return issueMapper.toResponse(savedIssue);
  }

  /** Leaves the issue unassigned entirely - the counterpart of {@code ProjectService#removeLeader}. */
  @Transactional
  public IssueResponse removeAssignee(Integer projectId, Integer issueId, Integer actorId) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setAssigneeUserId(null);
    issue.setAssigneeTeamId(null);

    Issue savedIssue = issueRepository.save(issue);

    publishChanges(projectId, actorId, before, savedIssue);

    return issueMapper.toResponse(savedIssue);
  }

  /** Soft delete: the row stays and {@code deletedAt} is stamped; the status is left where it was. */
  @Transactional
  public void deleteIssue(Integer projectId, Integer issueId, Integer actorId) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    // one reading, used for both the stamp and the event, so the two cannot disagree
    OffsetDateTime deletedAt = OffsetDateTime.now();

    issue.setDeletedAt(deletedAt);

    issueRepository.save(issue);

    eventPublisher.publishEvent(new IssueDeletedEvent(issueId, projectId, actorId, deletedAt));
  }
}
