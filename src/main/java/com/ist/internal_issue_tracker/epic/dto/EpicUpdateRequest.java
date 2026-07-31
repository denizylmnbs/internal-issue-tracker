package com.ist.internal_issue_tracker.epic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A full replacement of the editable fields. The status has its own endpoint, and the reporter is
 * not editable at all - see {@code Epic#reporterId}.
 */
public record EpicUpdateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    String description) {}
