package com.ist.internal_issue_tracker.project.dto;

/**
 * Someone who works on the project, by either route. This is the population {@code
 * ProjectDetailResponse#memberCount} counts - unlike {@link ProjectMemberResponse}, which lists only
 * the direct assignment rows that POST and DELETE act on.
 *
 * <p>{@code directlyAssigned} tells the two apart: when it is false the user is here through a team,
 * and removing them means removing the team - or the user from that team - not this project.
 */
public record ProjectParticipantResponse(Integer userId, boolean directlyAssigned) {}
