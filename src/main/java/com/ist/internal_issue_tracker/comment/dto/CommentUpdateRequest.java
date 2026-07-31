package com.ist.internal_issue_tracker.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The content is the only thing an edit may touch - the author and the issue are fixed. */
public record CommentUpdateRequest(
    @NotBlank(message = "Content cannot be blank") @Size(max = 5000) String content) {}
