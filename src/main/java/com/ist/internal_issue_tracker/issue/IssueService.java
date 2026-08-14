package com.ist.internal_issue_tracker.issue;

import com.ist.internal_issue_tracker.issue.dto
        .ChangeAssigneeRequest;
import com.ist.internal_issue_tracker.issue.dto.ChangeClassificationRequest;
import com.ist.internal_issue_tracker.issue.dto.ChangeEpicRequest;
import com.ist.internal_issue_tracker.issue.dto.ChangeSprintRequest;
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
import com.ist.internal_issue_tracker.shared.event.IssueDimensions;
import com.ist.internal_issue_tracker.shared.event.IssueFieldChange;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.exception.CommonErrorCode;
import com.ist.internal_issue_tracker.shared.port.EpicLookup;
import com.ist.internal_issue_tracker.shared.port.FieldDefinitionLookup;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import com.ist.internal_issue_tracker.shared.port.FieldSemantic;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.port.SprintLookup;
import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.security.Role;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Known limitation.</b> Soft-deleting a sprint or an epic does not clear it from the issues that
 * point at it, so an issue can name a sprint a caller can no longer fetch. Fixing it properly means
 * {@code sprint} reaching into {@code issues} on delete, which is exactly the cross-module write
 * the ports exist to prevent - it belongs to the {@code activity} work, where Modulith events come
 * in. Until then the reference is written once, validated at that moment, and left alone.
 *
 * <p><b>On {@code actorId}.</b> Every write takes the id of whoever is making the change,
 * separately from the reporter who filed the issue and the assignee it belongs to. Those two are
 * properties of the issue; the actor is a property of the <em>change</em>, and no column on {@code
 * issues} holds it - a lead moving someone else's issue to {@code DONE} leaves no trace of
 * themselves today. It is what {@code issue_activities.user_id} is written from. It always comes
 * from the authenticated principal and never from a request body, for the reason given on {@code
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
  private final FieldDefinitionLookup fieldDefinitionLookup;
  private final ApplicationEventPublisher eventPublisher;

  private void requireValidStatus(Integer projectId, String status) {
    if (!fieldDefinitionLookup.isValidCode(projectId, FieldKind.ISSUE_STATUS, status)) {
      throw new AppException(IssueErrorCode.ISSUE_STATUS_NOT_DEFINED);
    }
  }

  private void requireValidType(Integer projectId, String type) {
    if (!fieldDefinitionLookup.isValidCode(projectId, FieldKind.ISSUE_TYPE, type)) {
      throw new AppException(IssueErrorCode.ISSUE_TYPE_NOT_DEFINED);
    }
  }

  private void requireValidPriority(Integer projectId, String priority) {
    if (!fieldDefinitionLookup.isValidCode(projectId, FieldKind.ISSUE_PRIORITY, priority)) {
      throw new AppException(IssueErrorCode.ISSUE_PRIORITY_NOT_DEFINED);
    }
  }

  /** {@code resolvingUnit} is optional, so only a non-null value is checked. */
  private void requireValidResolvingUnitIfPresent(Integer projectId, String resolvingUnit) {
    if (resolvingUnit != null
        && !fieldDefinitionLookup.isValidCode(projectId, FieldKind.ISSUE_UNIT, resolvingUnit)) {
      throw new AppException(IssueErrorCode.ISSUE_UNIT_NOT_DEFINED);
    }
  }

  /**
   * The issue's type, priority, estimate and sprint as they stand right now, travelling with the
   * event so the activity row can freeze them - see {@link IssueDimensions}. Read from the saved
   * entity rather than from the request, so a field the request did not mention still reports what
   * the issue actually holds.
   *
   * <p>{@code type} and {@code priority} are already the raw codes {@code issue_activities} and the
   * metric queries store and match against - no {@code enum.name()} rendering needed now that both
   * are plain strings on {@link Issue} itself.
   */
  private static IssueDimensions dimensionsOf(Issue issue) {
    return new IssueDimensions(
        issue.getType(), issue.getPriority(), issue.getStoryPoint(), issue.getSprintId());
  }

  /**
   * Diffs the issue against the snapshot and publishes only if something moved - see {@code
   * IssueChangeDetector}. The clock is read here rather than in the listener, once per operation,
   * so that every row from one change carries the moment the change happened.
   */
  private void publishChanges(
      Integer projectId, Integer actorId, IssueSnapshot before, Issue after) {
    List<IssueFieldChange> changes = issueChangeDetector.diff(before, after);

    if (changes.isEmpty()) {
      return;
    }

    eventPublisher.publishEvent(
        new IssueChangedEvent(
            after.getId(), projectId, actorId, OffsetDateTime.now(), changes, dimensionsOf(after)));
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
   * Narrows {@code SecurityConfig}'s URL-level gate (editor / leader / participant) for the three
   * routes where "any participant" is too wide: an editor, the project's leader, or the issue's own
   * assignee may move its status or change who it is assigned to. The gate cannot express this
   * itself - its {@code AuthorizationManager}s only ever see the project id and the caller's id,
   * not the issue, so "is the caller this issue's assignee" can only be asked once the issue is
   * loaded, here.
   *
   * <p>A caller who is not a participant at all is still turned away by the URL-level gate before
   * this method runs, so the effective rule is {@code editor | leader | (participant & assignee)}.
   */
  private void requireEditorLeaderOrAssignee(Integer projectId, Integer actorId, Issue issue) {
    if (userLookup.hasAtLeastRole(actorId, Role.EDITOR)) {
      return;
    }

    if (projectLookup.isLeaderOfProject(projectId, actorId)) {
      return;
    }

    if (actorId.equals(issue.getAssigneeUserId())) {
      return;
    }

    throw new AppException(CommonErrorCode.FORBIDDEN);
  }

  /**
   * Assignees only have to exist and be active. Being a participant of the project is deliberately
   * <em>not</em> required - work is sometimes handed to someone outside the project for a day, and
   * refusing that would be a stricter rule than anyone asked for. Tightening it later is one call
   * to {@code ProjectLookup#isParticipantOfProject} in each branch.
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
    requireValidType(projectId, request.type());
    if (request.priority() != null) {
      requireValidPriority(projectId, request.priority());
    }
    requireValidResolvingUnitIfPresent(projectId, request.resolvingUnit());

    String defaultStatus = fieldDefinitionLookup.defaultCode(projectId, FieldKind.ISSUE_STATUS);
    String defaultPriority = fieldDefinitionLookup.defaultCode(projectId, FieldKind.ISSUE_PRIORITY);
    Issue issue =
        issueMapper.toEntity(projectId, reporterId, request, defaultStatus, defaultPriority);

    Issue savedIssue = issueRepository.save(issue);

    eventPublisher.publishEvent(
        new IssueCreatedEvent(
            savedIssue.getId(),
            projectId,
            reporterId,
            OffsetDateTime.now(),
            dimensionsOf(savedIssue)));

    return issueMapper.toResponse(savedIssue);
  }

  public IssueResponse getIssueById(Integer projectId, Integer issueId) {
    requireActiveProject(projectId);

    return issueMapper.toResponse(requireLiveIssue(projectId, issueId));
  }

  public PagedResponse<IssueResponse> getIssuesByProjectId(
      Integer projectId,
      String name,
      String type,
      String status,
      String priority,
      String resolvingUnit,
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
            resolvingUnit,
            sprintId,
            epicId,
            reporterId,
            assigneeUserId,
            assigneeTeamId,
            pageable);

    return PagedResponse.from(issues.map(issueMapper::toResponse));
  }

  /**
   * The snapshot is taken before {@code updateEntity} because that mutates the managed entity -
   * once it has run there is nothing left to compare against.
   */
  @Transactional
  public IssueResponse updateIssue(
      Integer projectId, Integer issueId, Integer actorId, IssueUpdateRequest request) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    requireValidPlacement(projectId, request.sprintId(), request.epicId());
    requireValidType(projectId, request.type());
    requireValidPriority(projectId, request.priority());
    requireValidResolvingUnitIfPresent(projectId, request.resolvingUnit());

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

    requireEditorLeaderOrAssignee(projectId, actorId, issue);
    requireValidStatus(projectId, request.status());

    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setStatus(request.status());

    Issue savedIssue = issueRepository.save(issue);

    // every cycle time, lead time and throughput number is built from the rows this line writes
    publishChanges(projectId, actorId, before, savedIssue);

    return issueMapper.toResponse(savedIssue);
  }

  /**
   * The narrow counterpart of {@link #updateIssue}: it moves the sprint and touches nothing else,
   * so a caller planning a sprint does not have to carry every other field through the round trip
   * to leave it where it was. (Previously this also forced the status to a hardcoded {@code TODO}
   * on every call, contradicting this method's own contract and {@code docs/API.md}'s "nothing else
   * on the issue is touched" - that line is dropped here rather than carried forward, since a
   * hardcoded target status has no meaning once statuses are project-defined data.)
   *
   * <p>The one case where the status does move with the sprint is {@link #returnToBacklog}: an
   * issue pulled out of every sprint is back in the backlog, and leaving it sitting at {@code TODO}
   * or {@code IN_PROGRESS} claims work is planned or underway when nothing schedules it any more.
   *
   * <p>The placement check is the same one {@code updateIssue} makes and is made for the same
   * reason - see {@link #requireValidPlacement}. The epic is passed as null because this call does
   * not propose one, not because it clears one.
   *
   * <p>No {@link #requireEditorLeaderOrAssignee}. Which sprint an issue sits in is a planning
   * decision rather than a report on one's own work, so it keeps {@code SecurityConfig}'s wider
   * gate - the same one {@code updateIssue} runs behind, since this is that method taken apart.
   */
  @Transactional
  public IssueResponse changeSprint(
      Integer projectId, Integer issueId, Integer actorId, ChangeSprintRequest request) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    requireValidPlacement(projectId, request.sprintId(), null);

    IssueSnapshot before = IssueSnapshot.of(issue);

    if (request.sprintId() == null && issue.getSprintId() != null) {
      returnToBacklog(projectId, issue);
    }

    issue.setSprintId(request.sprintId());

    Issue savedIssue = issueRepository.save(issue);

    publishChanges(projectId, actorId, before, savedIssue);

    return issueMapper.toResponse(savedIssue);
  }

  /**
   * Puts an issue leaving its last sprint back at the project's default issue status - the one the
   * project marked as what a brand new issue gets, which is what "backlog" <em>is</em> here. Naming
   * a literal {@code BACKLOG} would only work for projects that never renamed the seeded row.
   *
   * <p>Work that already finished or was called off is left alone: a {@code DONE} issue moved out
   * of a closed sprint is still done, and rewriting it to the default status would both lie about
   * the issue and take it back out of every completed-throughput count that reads the activity log.
   *
   * <p>The caller has taken its {@code before} snapshot by the time this runs, so the status move
   * is published as its own field change alongside the sprint one.
   */
  private void returnToBacklog(Integer projectId, Issue issue) {
    Set<String> terminal =
        new HashSet<>(
            fieldDefinitionLookup.codesWithSemantic(
                projectId, FieldKind.ISSUE_STATUS, FieldSemantic.DONE));
    terminal.addAll(
        fieldDefinitionLookup.codesWithSemantic(
            projectId, FieldKind.ISSUE_STATUS, FieldSemantic.CANCELLED));

    if (terminal.contains(issue.getStatus())) {
      return;
    }

    issue.setStatus(fieldDefinitionLookup.defaultCode(projectId, FieldKind.ISSUE_STATUS));
  }

  /**
   * As {@link #changeSprint}, for the epic. The one difference is silent: {@code publishChanges}
   * finds nothing to report, because the epic is the field {@code IssueSnapshot} deliberately omits
   * for want of an action type to record it under.
   */
  @Transactional
  public IssueResponse changeEpic(
      Integer projectId, Integer issueId, Integer actorId, ChangeEpicRequest request) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    requireValidPlacement(projectId, null, request.epicId());

    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setEpicId(request.epicId());

    Issue savedIssue = issueRepository.save(issue);

    publishChanges(projectId, actorId, before, savedIssue);

    return issueMapper.toResponse(savedIssue);
  }

  /**
   * Replaces the type, priority and estimate together - see {@link ChangeClassificationRequest} for
   * why those three and no others.
   *
   * <p>A caller changing one of them restates the other two as they stand, and the detector answers
   * with changes for whichever actually moved, so restating a value it already held costs nothing
   * and writes nothing.
   */
  @Transactional
  public IssueResponse changeClassification(
      Integer projectId, Integer issueId, Integer actorId, ChangeClassificationRequest request) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    requireValidType(projectId, request.type());
    requireValidPriority(projectId, request.priority());

    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setType(request.type());
    issue.setPriority(request.priority());
    issue.setStoryPoint(request.storyPoint());

    Issue savedIssue = issueRepository.save(issue);

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

    requireEditorLeaderOrAssignee(projectId, actorId, issue);
    requireValidAssignees(request.assigneeUserId(), request.assigneeTeamId());

    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setAssigneeUserId(request.assigneeUserId());
    issue.setAssigneeTeamId(request.assigneeTeamId());

    Issue savedIssue = issueRepository.save(issue);

    // one row, or two, or none - whichever of the pair actually moved
    publishChanges(projectId, actorId, before, savedIssue);

    return issueMapper.toResponse(savedIssue);
  }

  /**
   * Leaves the issue unassigned entirely - the counterpart of {@code ProjectService#removeLeader}.
   */
  @Transactional
  public IssueResponse removeAssignee(Integer projectId, Integer issueId, Integer actorId) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    requireEditorLeaderOrAssignee(projectId, actorId, issue);

    IssueSnapshot before = IssueSnapshot.of(issue);

    issue.setAssigneeUserId(null);
    issue.setAssigneeTeamId(null);

    Issue savedIssue = issueRepository.save(issue);

    publishChanges(projectId, actorId, before, savedIssue);

    return issueMapper.toResponse(savedIssue);
  }

  /**
   * Soft delete: the row stays and {@code deletedAt} is stamped; the status is left where it was.
   */
  @Transactional
  public void deleteIssue(Integer projectId, Integer issueId, Integer actorId) {
    requireActiveProject(projectId);

    Issue issue = requireLiveIssue(projectId, issueId);

    // one reading, used for both the stamp and the event, so the two cannot disagree
    OffsetDateTime deletedAt = OffsetDateTime.now();

    issue.setDeletedAt(deletedAt);

    issueRepository.save(issue);

    eventPublisher.publishEvent(
        new IssueDeletedEvent(issueId, projectId, actorId, deletedAt, dimensionsOf(issue)));
  }
}
