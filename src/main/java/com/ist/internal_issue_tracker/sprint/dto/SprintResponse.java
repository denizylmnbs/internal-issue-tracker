package com.ist.internal_issue_tracker.sprint.dto;

import com.ist.internal_issue_tracker.sprint.SprintStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Only live sprints are ever mapped into this record, so it carries no deletion field - there is
 * nothing a caller could learn from a {@code deletedAt} that is null on every row they can see.
 */
public record SprintResponse(
    Integer id,
    Integer projectId,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    SprintStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
