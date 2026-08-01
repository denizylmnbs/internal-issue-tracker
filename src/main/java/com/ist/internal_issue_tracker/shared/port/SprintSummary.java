package com.ist.internal_issue_tracker.shared.port;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * The part of a sprint a metric needs to describe it: enough to label a velocity row and to draw a
 * burndown's axis, and nothing else.
 *
 * <p>Deliberately not {@code SprintResponse}. That record belongs to the {@code sprint} module's web
 * layer and answers to its callers; this one is a contract between two modules and should change
 * only when a metric needs it to. Handing the response DTO across would tie the shape of an API
 * payload to the shape of a metric query.
 *
 * <p>{@code status} is a {@code String} rather than {@code SprintStatus} for the reason that governs
 * everything in this package: {@code shared} may not name a type from {@code sprint}. It holds
 * {@code enum.name()}.
 *
 * <p>{@code committedPoints} may be null, and null is not zero - see {@code Sprint}. A consumer that
 * renders it as zero would report a team as having missed a commitment it never made.
 */
public record SprintSummary(
    Integer id,
    String name,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    Integer committedPoints,
    OffsetDateTime committedAt) {}
