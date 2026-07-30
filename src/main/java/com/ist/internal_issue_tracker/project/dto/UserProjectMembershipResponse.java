package com.ist.internal_issue_tracker.project.dto;

import com.ist.internal_issue_tracker.project.ProjectStatus;

/**
 * A project seen from the user's side, the counterpart of {@code UserTeamMembershipResponse}. Only
 * live projects are ever mapped into this record, so it carries no {@code isActive} flag - but it
 * does carry {@code status}, which says something else entirely.
 */
public record UserProjectMembershipResponse(
    Integer projectId, String projectName, ProjectStatus projectStatus, boolean directlyAssigned) {}
