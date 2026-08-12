package com.ist.internal_issue_tracker.team.dto;

import com.ist.internal_issue_tracker.team.TeamField;
import java.time.OffsetDateTime;

/**
 * A membership seen from the user's side: which teams a user belongs to, with enough of the team
 * itself to render a list without a second call per row. Only active memberships are ever mapped
 * into this record, so it carries no {@code isActive} flag.
 *
 * <p>{@code joinedAt} is the date of the most recent join, not the first ever - someone who left
 * and came back reads as having joined on their return.
 */
public record UserTeamMembershipResponse(
    Integer membershipId,
    Integer teamId,
    String teamName,
    TeamField teamField,
    OffsetDateTime joinedAt) {}
