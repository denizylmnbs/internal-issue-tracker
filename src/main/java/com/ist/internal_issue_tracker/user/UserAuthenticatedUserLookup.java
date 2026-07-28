package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.security.AuthenticatedUserLookup;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the current authorities straight from the database on every request, so a role change or
 * deactivation takes effect immediately instead of waiting for the JWT to expire.
 */
@Component
@RequiredArgsConstructor
public class UserAuthenticatedUserLookup implements AuthenticatedUserLookup {

  private final UserRepository userRepository;

  @Override
  public Optional<AuthenticatedUser> findById(Integer userId) {
    return userRepository
        .findById(userId)
        .filter(User::getIsActive)
        .map(user -> new AuthenticatedUser(user.getId(), user.getRole()));
  }
}
