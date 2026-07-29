package com.ist.internal_issue_tracker.team.dto;

public record TeamMemberResponse(Integer id, Integer userId, Integer teamId, Boolean isActive) {}
