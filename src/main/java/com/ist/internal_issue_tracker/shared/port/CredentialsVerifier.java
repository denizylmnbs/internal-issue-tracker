package com.ist.internal_issue_tracker.shared.port;

import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;

/**
 * Port letting {@code auth} authenticate a login attempt without depending on the {@code user}
 * module. It was the one place left where a module reached for another module's service directly,
 * and the dependency was wider than the need: {@code AuthService} wanted one method and was handed
 * all of {@code UserService}, {@code deleteUser} and {@code changeRole} included.
 *
 * <p>Nothing of {@code user} leaks through it - {@link AuthenticatedUser} already lives in {@code
 * shared}, so the port is a clean cut rather than a wrapper around a foreign type.
 */
public interface CredentialsVerifier {

  /**
   * The authenticated caller, or a thrown {@code INVALID_CREDENTIALS} if the email is unknown, the
   * account is deactivated, or the password does not match. The three are deliberately
   * indistinguishable to the caller.
   */
  AuthenticatedUser verifyCredentials(String email, String rawPassword);
}
