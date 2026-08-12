package com.ist.internal_issue_tracker.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * What kind of work this is, how urgent it is and how big it is - the three a planner re-reads a
 * list to adjust, and the three the metrics slice by.
 *
 * <p>They travel together rather than as three endpoints because a replacement of one field alone
 * would need a body of one field, and three of those is three routes for one gesture. They are safe
 * to replace as a group in a way {@link IssueUpdateRequest}'s eight fields are not: none of them is
 * the description, so a caller restating all three from what it is displaying cannot silently
 * discard something long that someone else has just written.
 *
 * <p>{@code type} and {@code priority} are codes from this project's field definitions. {@code
 * storyPoint} is nullable and null means unestimated, on the same terms as {@link
 * ChangeSprintRequest}'s null.
 */
public record ChangeClassificationRequest(
    @NotBlank(message = "Type cannot be blank") String type,
    @NotBlank(message = "Priority cannot be blank") String priority,
    @PositiveOrZero(message = "Story point cannot be negative") Integer storyPoint) {}
