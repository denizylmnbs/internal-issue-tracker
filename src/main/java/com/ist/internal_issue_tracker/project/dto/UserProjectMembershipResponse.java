package com.ist.internal_issue_tracker.project.dto;

/**
 * A project seen from the user's side, the counterpart of {@code UserTeamMembershipResponse}. Only
 * live projects are ever mapped into this record, so it carries no {@code isActive} flag - but it
 * does carry {@code status}, a code from the global {@code PROJECT_STATUS} field definitions.
 */
public record UserProjectMembershipResponse(
    Integer projectId, String projectName, String projectStatus, boolean directlyAssigned) {}
