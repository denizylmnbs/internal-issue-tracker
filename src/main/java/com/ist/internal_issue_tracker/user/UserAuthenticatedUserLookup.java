package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.security.AuthenticatedUserLookup;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Resolves the current authorities for a user id, backed by the {@code auth-principal} Redis cache
 * (see {@code RedisConfig}) instead of hitting {@code users} on every request. A role change or
 * deactivation still takes effect immediately - not by re-reading the database, but because every
 * write path that can change either field evicts this cache's key for that user (see {@code
 * UserService#changeRole}, {@code UserService#deleteUser}, and the {@code
 * AuthPrincipalCacheEvictionListener} that reacts to their events). The two-minute TTL configured
 * there is a safety net for whatever isn't covered by explicit eviction, not the primary mechanism.
 */
@Component
@RequiredArgsConstructor
public class UserAuthenticatedUserLookup implements AuthenticatedUserLookup {

  private final UserRepository userRepository;

  /**
   * {@code unless} is load-bearing, not an optimization: Spring unwraps the {@code Optional} before
   * caching, so a not-found/inactive user would cache a bare {@code null} - and {@code RedisConfig}
   * disables null caching, which turns that into a thrown exception instead of a cache miss. It also
   * doubles as the negative-cache guard: an inactive or deleted user is simply never written here, so
   * there is no stale {@code true} to worry about invalidating on reactivation.
   */
  @Override
  @Cacheable(cacheNames = "auth-principal", key = "#userId", condition = "#userId != null",
      unless = "#result == null")
  public Optional<AuthenticatedUser> findById(Integer userId) {
    return userRepository
        .findById(userId)
        .filter(User::getIsActive)
        .map(user -> new AuthenticatedUser(user.getId(), user.getRole()));
  }
}
