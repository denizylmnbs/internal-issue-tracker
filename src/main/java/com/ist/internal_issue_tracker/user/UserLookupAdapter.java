package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.port.UserLookup;
import com.ist.internal_issue_tracker.shared.security.AuthenticatedUserLookup;
import com.ist.internal_issue_tracker.shared.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Backs {@link UserLookup} with {@link AuthenticatedUserLookup} rather than the repository
 * directly, so both existence and role checks ride the {@code auth-principal} Redis cache instead
 * of issuing their own {@code users} queries. Going through the injected bean (not a same-package
 * {@code new UserAuthenticatedUserLookup(...)}) matters: {@code @Cacheable} only applies through
 * the Spring proxy.
 */
@Component
@RequiredArgsConstructor
public class UserLookupAdapter implements UserLookup {

  private final AuthenticatedUserLookup authenticatedUserLookup;

  @Override
  public boolean existsActiveUser(Integer userId) {
    return userId != null && authenticatedUserLookup.findById(userId).isPresent();
  }

  @Override
  public boolean hasAtLeastRole(Integer userId, Role minRole) {
    return userId != null
        && authenticatedUserLookup
            .findById(userId)
            .map(user -> user.getRole())
            .filter(role -> role.atLeast(minRole))
            .isPresent();
  }
}
