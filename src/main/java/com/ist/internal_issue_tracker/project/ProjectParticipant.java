package com.ist.internal_issue_tracker.project;

/**
 * Row shape of the participant union query. An interface projection rather than a record because the
 * query is native, and Hibernate can only map a constructor expression from HQL.
 */
public interface ProjectParticipant {

  Integer getUserId();

  /**
   * {@code true} when the user holds a {@code project_users} row of their own. A user can be both
   * directly assigned and reachable through a team; this reports the direct assignment, which is the
   * only one {@code DELETE .../members/{userId}} can take away.
   */
  Boolean getDirectlyAssigned();
}
