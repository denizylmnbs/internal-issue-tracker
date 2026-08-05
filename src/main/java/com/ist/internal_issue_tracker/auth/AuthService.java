package com.ist.internal_issue_tracker.auth;

import com.ist.internal_issue_tracker.auth.dto.LoginRequest;
import com.ist.internal_issue_tracker.auth.dto.LoginResponse;
import com.ist.internal_issue_tracker.auth.dto.RefreshTokenRequest;
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
  private final RefreshTokenService refreshTokenService;

  public LoginResponse login(LoginRequest request) {
    AuthenticatedUser authenticatedUser =
        credentialsVerifier.verifyCredentials(request.email(), request.password());
    String accessToken = jwtService.generateToken(authenticatedUser.getId());
    String refreshToken = refreshTokenService.issue(authenticatedUser.getId());
    return new LoginResponse(accessToken, refreshToken);
  }

  /**
   * Exchanges a refresh token for a new access/refresh pair. The old refresh token is consumed by
   * {@link RefreshTokenService#rotate} as part of the lookup, so it cannot be replayed - see that
   * method for what happens if it already was.
   */
  public LoginResponse refresh(RefreshTokenRequest request) {
    RefreshTokenService.RotationResult result = refreshTokenService.rotate(request.refreshToken());
    String accessToken = jwtService.generateToken(result.userId());
    return new LoginResponse(accessToken, result.refreshToken());
  }

  /** Revokes the given refresh token so it can no longer be used to mint access tokens. */
  public void logout(RefreshTokenRequest request) {
    refreshTokenService.revoke(request.refreshToken());
  }
}
