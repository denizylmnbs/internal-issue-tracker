package com.ist.internal_issue_tracker.auth;

import com.ist.internal_issue_tracker.auth.dto.LoginRequest;
import com.ist.internal_issue_tracker.auth.dto.LoginResponse;
import com.ist.internal_issue_tracker.shared.port.CredentialsVerifier;
import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final CredentialsVerifier credentialsVerifier;
  private final JwtService jwtService;

  public LoginResponse login(LoginRequest request) {
    AuthenticatedUser authenticatedUser =
        credentialsVerifier.verifyCredentials(request.email(), request.password());
    String accessToken = jwtService.generateToken(authenticatedUser.getId());
    return new LoginResponse(accessToken);
  }
}
