package com.ist.internal_issue_tracker.project.dto;

import com.ist.internal_issue_tracker.project.ProjectStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * A single project with the two rollups that only make sense one at a time. They are kept out of
 * {@link ProjectResponse} on purpose: the list endpoint would have to run two extra counts per row
 * to fill them in.
 *
 * <p>{@code memberCount} follows the project's membership rule - directly assigned users plus
 * everyone reached through an assigned team, counted once.
 */
public record ProjectDetailResponse(
    Integer id,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    Integer leaderId,
    ProjectStatus status,
    Boolean isActive,
    long memberCount,
    long teamCount,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
