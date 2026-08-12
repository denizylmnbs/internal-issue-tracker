package com.ist.internal_issue_tracker.epic;

import com.ist.internal_issue_tracker.epic.dto.ChangeStatusRequest;
import com.ist.internal_issue_tracker.epic.dto.EpicCreateRequest;
import com.ist.internal_issue_tracker.epic.dto.EpicResponse;
import com.ist.internal_issue_tracker.epic.dto.EpicUpdateRequest;
import com.ist.internal_issue_tracker.epic.exception.EpicErrorCode;
import com.ist.internal_issue_tracker.epic.exception.EpicNameAlreadyExistsException;
import com.ist.internal_issue_tracker.epic.exception.EpicNotFoundException;
import com.ist.internal_issue_tracker.epic.mapper.EpicMapper;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EpicService {

  private final EpicRepository epicRepository;
  private final EpicMapper epicMapper;
  private final ProjectLookup projectLookup;

  /**
   * Every path into this service starts here, so an epic on a soft-deleted project is unreachable
   * without any of the queries below having to know that projects can be deleted at all.
   */
  private void requireActiveProject(Integer projectId) {
    if (!projectLookup.existsActiveProject(projectId)) {
      throw new AppException(EpicErrorCode.PROJECT_NOT_FOUND);
    }
  }

  /** Both keys - see {@code EpicRepository#findByIdAndProjectIdAndDeletedAtIsNull}. */
  private Epic requireLiveEpic(Integer projectId, Integer epicId) {
    return epicRepository
        .findByIdAndProjectIdAndDeletedAtIsNull(epicId, projectId)
        .orElseThrow(() -> new EpicNotFoundException(epicId));
  }

  /**
   * {@code reporterId} is the authenticated caller, handed down from the controller rather than
   * read from the request. It is not checked against {@code UserLookup}: the request only got this
   * far because {@code JwtAuthenticationFilter} resolved the principal through {@code
   * AuthenticatedUserLookup} on this very request, so the user provably exists and is active.
   */
  public EpicResponse createEpic(Integer projectId, Integer reporterId, EpicCreateRequest request) {
    requireActiveProject(projectId);

    if (epicRepository.existsByProjectIdAndNameAndDeletedAtIsNull(projectId, request.name())) {
      throw new EpicNameAlreadyExistsException(request.name());
    }

    // status is left at the entity's TODO default
    Epic epic = epicMapper.toEntity(projectId, reporterId, request);

    Epic savedEpic;
    try {
      savedEpic = epicRepository.save(epic);
    } catch (DataIntegrityViolationException e) {
      // the check above lost a race; the name is the only constraint an insert can trip
      throw new EpicNameAlreadyExistsException(request.name());
    }

    return epicMapper.toResponse(savedEpic);
  }

  public EpicResponse getEpicById(Integer projectId, Integer epicId) {
    requireActiveProject(projectId);

    return epicMapper.toResponse(requireLiveEpic(projectId, epicId));
  }

  public PagedResponse<EpicResponse> getEpicsByProjectId(
      Integer projectId, String name, EpicStatus status, Integer reporterId, Pageable pageable) {
    requireActiveProject(projectId);

    // derived query, so the caller's sort is honoured as-is
    Page<Epic> epics =
        epicRepository.findAllByFilters(projectId, name, status, reporterId, pageable);

    return PagedResponse.from(epics.map(epicMapper::toResponse));
  }

  public EpicResponse updateEpic(Integer projectId, Integer epicId, EpicUpdateRequest request) {
    requireActiveProject(projectId);

    Epic epic = requireLiveEpic(projectId, epicId);

    if (epicRepository.existsByProjectIdAndNameAndDeletedAtIsNullAndIdNot(
        projectId, request.name(), epicId)) {
      throw new EpicNameAlreadyExistsException(request.name());
    }

    epicMapper.updateEntity(epic, request);

    Epic savedEpic;
    try {
      savedEpic = epicRepository.save(epic);
    } catch (DataIntegrityViolationException e) {
      throw new EpicNameAlreadyExistsException(request.name());
    }

    return epicMapper.toResponse(savedEpic);
  }

  /**
   * Any status may follow any other - an epic can be revived out of {@code CANCELLED} or reopened
   * from {@code COMPLETED}. There is no database constraint to translate here either: {@code epics}
   * carries nothing like the sprint table's "one in progress per project" index. If this ever needs
   * narrowing, this is the one place it has to happen.
   */
  public EpicResponse changeStatus(Integer projectId, Integer epicId, ChangeStatusRequest request) {
    requireActiveProject(projectId);

    Epic epic = requireLiveEpic(projectId, epicId);

    epic.setStatus(request.status());

    return epicMapper.toResponse(epicRepository.save(epic));
  }

  /**
   * Soft delete: the row stays and {@code deletedAt} is stamped. The status is left where it was,
   * so the record still says what the epic was doing when it was dropped.
   */
  public void deleteEpic(Integer projectId, Integer epicId) {
    requireActiveProject(projectId);

    Epic epic = requireLiveEpic(projectId, epicId);

    epic.setDeletedAt(OffsetDateTime.now());

    epicRepository.save(epic);
  }
}
