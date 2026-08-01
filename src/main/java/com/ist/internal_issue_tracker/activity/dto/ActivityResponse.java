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
 */
public record ActivityResponse(
    Integer id,
    Integer userId,
    String actionType,
    String oldValue,
    String newValue,
    OffsetDateTime createdAt) {}
