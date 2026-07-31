package com.ist.internal_issue_tracker.comment.dto;

import java.time.OffsetDateTime;

/**
 * Only live comments are ever mapped into this record, so it carries no deletion field.
 *
 * <p>{@code updatedAt} is worth reading here rather than being boilerplate: a comment whose
 * {@code updatedAt} has moved past its {@code createdAt} has been edited since it was written, which
 * is the only signal a reader gets that the words changed.
 */
public record CommentResponse(
    Integer id,
    Integer issueId,
    Integer userId,
    String content,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
