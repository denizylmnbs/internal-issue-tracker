package com.ist.internal_issue_tracker.team.dto;

import java.time.OffsetDateTime;

/**
 * A membership seen from the team's side. {@code joinedAt} is the most recent join rather than the
 * first ever, for the reason given on {@link UserTeamMembershipResponse} - it is the row's {@code
 * updatedAt}, and joining or leaving is the only thing that writes to one of these rows.
 */
public record TeamMemberResponse(
    Integer id, Integer userId, Integer teamId, Boolean isActive, OffsetDateTime joinedAt) {}
