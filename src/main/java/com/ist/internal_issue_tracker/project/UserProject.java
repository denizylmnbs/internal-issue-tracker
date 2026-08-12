package com.ist.internal_issue_tracker.project;

/**
 * Row shape of the "projects this user works on" union query, the mirror of {@link
 * ProjectParticipant}. An interface projection for the same reason: the query is native.
 *
 * <p>{@code status} comes back as the raw column value; turning it into {@code ProjectStatus} is
 * the mapper's job, because a native projection cannot convert it.
 */
public interface UserProject {

  Integer getProjectId();

  String getProjectName();

  String getProjectStatus();

  /** {@code true} when the user is assigned to the project in their own right, not via a team. */
  Boolean getDirectlyAssigned();
}
