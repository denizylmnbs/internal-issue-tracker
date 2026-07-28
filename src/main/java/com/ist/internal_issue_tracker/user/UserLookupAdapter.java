package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.port.UserLookup;
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
}
