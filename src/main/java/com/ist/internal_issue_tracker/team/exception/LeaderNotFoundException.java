package com.ist.internal_issue_tracker.team.exception;

import com.ist.internal_issue_tracker.shared.exception.AppException;

/** The candidate leader does not exist or is no longer active, so it cannot lead a team. */
public class LeaderNotFoundException extends AppException {

  public LeaderNotFoundException(Integer leaderId) {
    super(TeamErrorCode.LEADER_NOT_FOUND, "User with id " + leaderId + " cannot lead a team");
  }
}
