package com.ist.internal_issue_tracker.project.dto;

import jakarta.validation.constraints.NotNull;

/**
 * The project comes from the {@code {id}} path variable - see {@link ProjectMemberCreateRequest}.
 */
public record ProjectTeamCreateRequest(
    @NotNull(message = "Team id cannot be null") Integer teamId) {}
