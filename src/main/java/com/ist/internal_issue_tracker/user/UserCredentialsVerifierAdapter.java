package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.port.CredentialsVerifier;
import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Delegates rather than reimplements: the check needs {@code PasswordHasher}, which is {@code
 * user}'s internal, so the logic stays in {@link UserService} and this only narrows what {@code
 * auth} can see to the one method it uses.
 */
@Component
@RequiredArgsConstructor
public class UserCredentialsVerifierAdapter implements CredentialsVerifier {

  private final UserService userService;

  @Override
  public AuthenticatedUser verifyCredentials(String email, String rawPassword) {
    return userService.verifyCredentials(email, rawPassword);
  }
}
