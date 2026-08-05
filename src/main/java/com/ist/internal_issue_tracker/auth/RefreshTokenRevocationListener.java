package com.ist.internal_issue_tracker.auth;

import com.ist.internal_issue_tracker.shared.event.UserCredentialsChangedEvent;
import com.ist.internal_issue_tracker.shared.event.UserDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps refresh tokens from outliving the account state they were issued under. A session started
 * before a password change, a reset, or a deactivation must not be able to keep renewing itself
 * past that change - access tokens already get this for free from {@code
 * UserAuthenticatedUserLookup} re-reading {@code isActive}/{@code role} on every request, but
 * refresh tokens are looked up by hash alone and know nothing about the account, so revocation has
 * to be pushed to them explicitly.
 *
 * <p>Plain {@code @EventListener}, not {@code @ApplicationModuleListener}: this is the same
 * synchronous-cleanup choice {@code TeamMembershipCleanupListener} makes for {@code
 * UserDeactivatedEvent} - an async gap here would leave a compromised or just-changed credential's
 * refresh tokens usable for however long delivery takes.
 */
@Component
@RequiredArgsConstructor
class RefreshTokenRevocationListener {

  private final RefreshTokenService refreshTokenService;

  @EventListener
  void onUserDeactivated(UserDeactivatedEvent event) {
    refreshTokenService.revokeAllForUser(event.userId());
  }

  @EventListener
  void onUserCredentialsChanged(UserCredentialsChangedEvent event) {
    refreshTokenService.revokeAllForUser(event.userId());
  }
}
