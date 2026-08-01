package com.ist.internal_issue_tracker.sprint.dto;

import com.ist.internal_issue_tracker.sprint.SprintStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Only live sprints are ever mapped into this record, so it carries no deletion field - there is
 * nothing a caller could learn from a {@code deletedAt} that is null on every row they can see.
 *
 * <p>{@code committedPoints} and {@code committedAt} are readable but not writable: no request DTO
 * carries them, and the only thing that sets them is the sprint starting. See {@code Sprint} for
 * why. Both are null until then, and stay null forever on a sprint that was started before the
 * column existed.
 */
public record SprintResponse(
    Integer id,
    Integer projectId,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    SprintStatus status,
    Integer committedPoints,
    OffsetDateTime committedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
