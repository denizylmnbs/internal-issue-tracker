package com.ist.internal_issue_tracker.sprint;

import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.sprint.dto.ChangeStatusRequest;
import com.ist.internal_issue_tracker.sprint.dto.SprintCreateRequest;
import com.ist.internal_issue_tracker.sprint.dto.SprintResponse;
import com.ist.internal_issue_tracker.sprint.dto.SprintUpdateRequest;
import com.ist.internal_issue_tracker.sprint.exception.SprintErrorCode;
import com.ist.internal_issue_tracker.sprint.exception.SprintNameAlreadyExistsException;
import com.ist.internal_issue_tracker.sprint.exception.SprintNotFoundException;
import com.ist.internal_issue_tracker.sprint.mapper.SprintMapper;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SprintService {

  private final SprintRepository sprintRepository;
  private final SprintMapper sprintMapper;
  private final ProjectLookup projectLookup;

  /**
   * Every path into this service starts here, so a sprint on a soft-deleted project is unreachable
   * without any of the queries below having to know that projects can be deleted at all.
   */
  private void requireActiveProject(Integer projectId) {
    if (!projectLookup.existsActiveProject(projectId)) {
      throw new AppException(SprintErrorCode.PROJECT_NOT_FOUND);
    }
  }

  /** Both keys - see {@code SprintRepository#findByIdAndProjectIdAndDeletedAtIsNull}. */
  private Sprint requireLiveSprint(Integer projectId, Integer sprintId) {
    return sprintRepository
        .findByIdAndProjectIdAndDeletedAtIsNull(sprintId, projectId)
        .orElseThrow(() -> new SprintNotFoundException(sprintId));
  }

  public SprintResponse createSprint(Integer projectId, SprintCreateRequest request) {
    requireActiveProject(projectId);

    if (sprintRepository.existsByProjectIdAndNameAndDeletedAtIsNull(projectId, request.name())) {
      throw new SprintNameAlreadyExistsException(request.name());
    }

    // status is left at the entity's TODO default
    Sprint sprint = sprintMapper.toEntity(projectId, request);

    Sprint savedSprint;
    try {
      savedSprint = sprintRepository.save(sprint);
    } catch (DataIntegrityViolationException e) {
      // the check above lost a race; a new sprint cannot trip the IN_PROGRESS index
      throw new SprintNameAlreadyExistsException(request.name());
    }

    return sprintMapper.toResponse(savedSprint);
  }

  public SprintResponse getSprintById(Integer projectId, Integer sprintId) {
    requireActiveProject(projectId);

    return sprintMapper.toResponse(requireLiveSprint(projectId, sprintId));
  }

  public PagedResponse<SprintResponse> getSprintsByProjectId(
      Integer projectId, String name, SprintStatus status, Pageable pageable) {
    requireActiveProject(projectId);

    // derived query, so the caller's sort is honoured as-is
    Page<Sprint> sprints = sprintRepository.findAllByFilters(projectId, name, status, pageable);

    return PagedResponse.from(sprints.map(sprintMapper::toResponse));
  }

  public SprintResponse updateSprint(
      Integer projectId, Integer sprintId, SprintUpdateRequest request) {
    requireActiveProject(projectId);

    Sprint sprint = requireLiveSprint(projectId, sprintId);

    if (sprintRepository.existsByProjectIdAndNameAndDeletedAtIsNullAndIdNot(
        projectId, request.name(), sprintId)) {
      throw new SprintNameAlreadyExistsException(request.name());
    }

    sprintMapper.updateEntity(sprint, request);

    Sprint savedSprint;
    try {
      savedSprint = sprintRepository.save(sprint);
    } catch (DataIntegrityViolationException e) {
      // the name is the only constraint this write can trip - the status is not touched here
      throw new SprintNameAlreadyExistsException(request.name());
    }

    return sprintMapper.toResponse(savedSprint);
  }

  /**
   * Any status may follow any other, with one exception the database owns: a project may hold only
   * one {@code IN_PROGRESS} sprint at a time. That is not a transition rule - going back to {@code
   * TODO} from {@code COMPLETED} is fine - it is a rule about how many sprints may sit in one status
   * at once, and it is checked here only so callers get a named 409 rather than a raw constraint
   * violation.
   *
   * <p>The check is a pre-check, not a guarantee: two requests can pass it at the same time and the
   * partial index is what actually decides, so the save is wrapped to report the loser the same way.
   */
  public SprintResponse changeStatus(
      Integer projectId, Integer sprintId, ChangeStatusRequest request) {
    requireActiveProject(projectId);

    Sprint sprint = requireLiveSprint(projectId, sprintId);

    boolean startingAnother =
        request.status() == SprintStatus.IN_PROGRESS && sprint.getStatus() != SprintStatus.IN_PROGRESS;

    if (startingAnother
        && sprintRepository.existsByProjectIdAndStatusAndDeletedAtIsNull(
            projectId, SprintStatus.IN_PROGRESS)) {
      throw new AppException(SprintErrorCode.SPRINT_ALREADY_IN_PROGRESS);
    }

    sprint.setStatus(request.status());

    Sprint savedSprint;
    try {
      savedSprint = sprintRepository.save(sprint);
    } catch (DataIntegrityViolationException e) {
      // one_active_sprint_per_project: another request started a sprint first
      throw new AppException(SprintErrorCode.SPRINT_ALREADY_IN_PROGRESS);
    }

    return sprintMapper.toResponse(savedSprint);
  }

  /**
   * Soft delete: the row stays and {@code deletedAt} is stamped. The status is left exactly where it
   * was, so the record still says what the sprint was doing when it was dropped.
   *
   * <p>That is only safe because {@code one_active_sprint_per_project} is partial on {@code
   * deleted_at} as well as on the status. Were it not, a deleted row still reading {@code
   * IN_PROGRESS} would go on holding the project's only slot - invisibly, since every read filters
   * deleted rows out - and the project could never start another sprint.
   */
  public void deleteSprint(Integer projectId, Integer sprintId) {
    requireActiveProject(projectId);

    Sprint sprint = requireLiveSprint(projectId, sprintId);

    sprint.setDeletedAt(OffsetDateTime.now());

    sprintRepository.save(sprint);
  }
}
