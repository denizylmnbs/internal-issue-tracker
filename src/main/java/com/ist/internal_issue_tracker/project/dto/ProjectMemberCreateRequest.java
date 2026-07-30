package com.ist.internal_issue_tracker.project.dto;

import jakarta.validation.constraints.NotNull;

/**
 * The project is identified by the {@code {id}} path variable rather than by a field here: {@code
 * SecurityConfig}'s {@code editorOrProjectLeader} rule has to know which project is being modified
 * before the controller runs, and an {@code AuthorizationManager} can only read path variables -
 * reading the body would consume the stream before deserialization.
 */
public record ProjectMemberCreateRequest(
    @NotNull(message = "User id cannot be null") Integer userId) {}
