package com.ist.internal_issue_tracker.activity.dto;

import java.time.OffsetDateTime;

/**
 * One row of history, in the one shape all three tables share.
 *
 * <p>A single record rather than three near-identical ones: the tables differ only in which parent
 * they hang off, and that parent is already in the path of the endpoint that returns it. Repeating
 * it in the body would be repeating the question in the answer.
 *
 * <p>{@code actionType} is the raw enum name rather than a sentence. Rendering it is the client's
 * job, which is what lets the wording change without a version of this record changing with it.
 *
 * <p>{@code userId} is the actor, and it is an id rather than a name: resolving it here would mean
 * this module reading {@code users}, and a name copied into a history row goes stale the moment it
 * is edited.
 *
 * <p>{@code scope} and {@code subjectId} say which of the three tables a row came from and what it
 * hangs off (a project id, an issue id, or a sprint id). They are redundant on the three
 * single-subject endpoints — every row on {@code GET .../issues/{issueId}/activities} is obviously
 * {@code ISSUE}/{@code issueId} — but required on the project-wide feed
 * ({@code GET /api/projects/{id}/activities}), which unions all three tables: without them a client
 * cannot tell a project's own {@code STATUS_UPDATED} row from an issue's, or route a click on one
 * row to the right page. {@code id} alone cannot disambiguate either, since it is only unique within
 * one table.
 */
public record ActivityResponse(
    Integer id,
    Integer userId,
    String actionType,
    String oldValue,
    String newValue,
    OffsetDateTime createdAt,
    String scope,
    Integer subjectId) {}
