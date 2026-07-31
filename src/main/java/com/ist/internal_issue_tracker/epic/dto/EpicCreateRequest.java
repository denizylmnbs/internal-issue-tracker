package com.ist.internal_issue_tracker.epic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Neither the project nor the reporter is a field here. The project comes from the path and the
 * reporter from the authenticated caller, so there is exactly one source for each and no way for a
 * caller to file an epic under someone else's name.
 *
 * <p>The status is absent for the reason it is on {@code SprintCreateRequest}: every epic starts in
 * {@code TODO} and moves on through its own endpoint.
 */
public record EpicCreateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    String description) {}
