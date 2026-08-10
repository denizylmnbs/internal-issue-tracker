package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.event.UserDeactivatedEvent;
import com.ist.internal_issue_tracker.shared.event.UserRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps {@code UserAuthenticatedUserLookup}'s {@code auth-principal} cache honest. That cache is
 * keyed by {@code userId} and holds the {@code (id, role)} pair every authenticated request is
 * authorized against, so a role change or deactivation has to evict the one key it affects
 * immediately rather than ride out the two-minute TTL - the same reasoning {@code
 * ProjectParticipantCacheEvictionListener} applies to project/team membership.
 *
 * <p>Plain {@code @EventListener}, and these events stay in the process: this runs inline in the
 * publisher's transaction, the same choice {@code RefreshTokenRevocationListener} makes for these
 * same events - an async gap here would leave a demoted or deactivated user's cached grant usable
 * for however long delivery takes.
 */
@Component
@RequiredArgsConstructor
class AuthPrincipalCacheEvictionListener {

  private final CacheManager cacheManager;

  private void evict(Integer userId) {
    Cache cache = cacheManager.getCache("auth-principal");
    if (cache != null) {
      cache.evict(userId);
    }
  }

  @EventListener
  void onUserRoleChanged(UserRoleChangedEvent event) {
    evict(event.userId());
  }

  @EventListener
  void onUserDeactivated(UserDeactivatedEvent event) {
    evict(event.userId());
  }
}
