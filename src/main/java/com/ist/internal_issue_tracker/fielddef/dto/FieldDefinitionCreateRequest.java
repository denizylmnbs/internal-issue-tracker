package com.ist.internal_issue_tracker.fielddef.dto;

import com.ist.internal_issue_tracker.shared.port.FieldKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code code} is validated as upper-snake-case because it is what lands in {@code
 * issues.status}/{@code issue_activities.new_value}/Kafka event payloads verbatim - the same
 * shape every existing enum name already had, which is what lets the six kinds keep working
 * without a translation step anywhere downstream.
 */
public record FieldDefinitionCreateRequest(
    @NotNull(message = "Kind cannot be null") FieldKind kind,
    @NotBlank(message = "Code cannot be blank")
        @Size(min = 1, max = 30)
        @Pattern(regexp = "[A-Z][A-Z0-9_]*", message = "Code must be UPPER_SNAKE_CASE")
        String code,
    @NotBlank(message = "Label cannot be blank") @Size(min = 1, max = 100) String label,
    @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Color must be a #RRGGBB hex value") String color,
    boolean isDefault,
    boolean isDone,
    boolean isCancelled,
    boolean isActiveWork,
    boolean isDefect) {}
