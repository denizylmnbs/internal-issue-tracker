package com.ist.internal_issue_tracker.fielddef.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** No {@code code} - it is immutable once created, see {@code FieldDefinition}. */
public record FieldDefinitionUpdateRequest(
    @NotBlank(message = "Label cannot be blank") @Size(min = 1, max = 100) String label,
    @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Color must be a #RRGGBB hex value") String color,
    boolean isDefault,
    boolean isDone,
    boolean isCancelled,
    boolean isActiveWork,
    boolean isDefect) {}
