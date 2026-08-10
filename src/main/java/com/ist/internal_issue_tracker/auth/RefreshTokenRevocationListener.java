package com.ist.internal_issue_tracker.auth;

import com.ist.internal_issue_tracker.shared.event.UserCredentialsChangedEvent;
import com.ist.internal_issue_tracker.shared.event.UserDeactivatedEvent;
import com.ist.internal_issue_tracker.shared.event.UserRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps refresh tokens from outliving the account state they were issued under. A session started
 * before a password change, a reset, a role change, or a deactivation must not be able to keep
 * renewing itself past that change - access tokens already get this for free from {@code
 * UserAuthenticatedUserLookup}'s {@code auth-principal} cache being evicted on the same events (see
 * {@code AuthPrincipalCacheEvictionListener}), but refresh tokens are looked up by hash alone and
 * know nothing about the account, so revocation has to be pushed to them explicitly.
 *
 * <p>Plain {@code @EventListener}, and the events it consumes are not externalised: this is the
 * same synchronous-cleanup choice {@code TeamMembershipCleanupListener} makes for {@code
 * UserDeactivatedEvent} - a trip through a broker here would leave a compromised, just-changed, or
 * just-demoted credential's refresh tokens usable for however long delivery takes.
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

  @EventListener
  void onUserRoleChanged(UserRoleChangedEvent event) {
    refreshTokenService.revokeAllForUser(event.userId());
  }
}
