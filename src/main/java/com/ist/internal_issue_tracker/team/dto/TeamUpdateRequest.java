package com.ist.internal_issue_tracker.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Full replacement of a team's own details. The leader is deliberately absent - changing it is a
 * separate, admin-only operation ({@code PATCH /api/teams/{id}/leader}). {@code field} is a code
 * from the global {@code TEAM_FIELD} field definitions.
 */
public record TeamUpdateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    String field) {}
