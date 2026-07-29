package com.ist.internal_issue_tracker.team.exception;

import com.ist.internal_issue_tracker.shared.exception.DuplicateResourceException;

/** Another team already uses the given name — {@code teams.name} is unique. */
public class TeamNameAlreadyExistsException extends DuplicateResourceException {

  public TeamNameAlreadyExistsException(String name) {
    super(TeamErrorCode.TEAM_NAME_ALREADY_EXISTS, "This team name already exists: " + name);
  }
}
