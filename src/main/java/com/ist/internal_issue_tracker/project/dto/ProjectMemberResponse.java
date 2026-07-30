package com.ist.internal_issue_tracker.project.dto;

public record ProjectMemberResponse(
    Integer id, Integer userId, Integer projectId, Boolean isActive) {}
