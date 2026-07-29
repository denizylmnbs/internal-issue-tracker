package com.ist.internal_issue_tracker.team.dto;

import com.ist.internal_issue_tracker.team.TeamField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Full replacement of a team's own details. The leader is deliberately absent - changing it is a
 * separate, admin-only operation ({@code PATCH /api/teams/{id}/leader}).
 */
public record TeamUpdateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    TeamField field) {}
