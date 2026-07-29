package com.ist.internal_issue_tracker.team.dto;

import com.ist.internal_issue_tracker.team.TeamField;
import java.time.OffsetDateTime;

public record TeamResponse(
    Integer id,
    String name,
    TeamField field,
    Integer leaderId,
    Boolean isActive,
    OffsetDateTime createdAt) {}
