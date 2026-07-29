package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Backs {@link UserLookup} with the {@code user} module's own repository. */
@Component
@RequiredArgsConstructor
public class UserLookupAdapter implements UserLookup {

  private final UserRepository userRepository;

  @Override
  public boolean existsActiveUser(Integer userId) {
    return userId != null && userRepository.existsByIdAndIsActiveTrue(userId);
  }

  @Override
  public boolean hasAtLeastRole(Integer userId, Role minRole) {
    return userId != null
        && userRepository
            .findActiveRoleById(userId)
            .filter(role -> role.atLeast(minRole))
            .isPresent();
  }
}
