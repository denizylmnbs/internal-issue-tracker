package com.ist.internal_issue_tracker.project.dto;

public record ProjectTeamResponse(
    Integer id, Integer teamId, Integer projectId, Boolean isActive) {}
