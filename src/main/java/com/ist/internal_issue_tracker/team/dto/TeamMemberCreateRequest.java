package com.ist.internal_issue_tracker.team.dto;

import jakarta.validation.constraints.NotNull;

/**
 * The team is identified by the {@code {id}} path variable rather than by a field here: {@code
 * SecurityConfig}'s {@code editorOrTeamLeader} rule has to know which team is being modified before
 * the controller runs, and an {@code AuthorizationManager} can only read path variables - reading
 * the body would consume the stream before deserialization.
 */
public record TeamMemberCreateRequest(
    @NotNull(message = "User id cannot be null") Integer userId) {}
