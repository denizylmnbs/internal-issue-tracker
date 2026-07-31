package com.ist.internal_issue_tracker.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One field, because everything else is already known: the issue comes from the path and the author
 * from the authenticated caller. Neither is accepted from the body, so neither can be forged.
 */
public record CommentCreateRequest(
    @NotBlank(message = "Content cannot be blank") @Size(max = 5000) String content) {}
