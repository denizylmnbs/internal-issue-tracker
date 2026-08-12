package com.ist.internal_issue_tracker.fielddef.dto;

import com.ist.internal_issue_tracker.shared.port.FieldKind;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * {@code kind} is required rather than inferred from the ids: it is what lets the service reject,
 * in one check, an id list that mixes kinds or omits one of the kind's active rows, instead of
 * discovering the mismatch row by row.
 */
public record ReorderRequest(
    @NotNull(message = "Kind cannot be null") FieldKind kind,
    @NotEmpty(message = "orderedIds cannot be empty") List<Integer> orderedIds) {}
