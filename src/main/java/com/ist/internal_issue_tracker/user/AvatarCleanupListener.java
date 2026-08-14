package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.event.UserDeactivatedEvent;
import com.ist.internal_issue_tracker.shared.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Clears a deactivated user's avatar. No new event needed - {@link UserDeactivatedEvent} already
 * exists and {@code UserService.deleteUser} already publishes it.
 *
 * <p>Deactivation is terminal in this app (there is no reactivate endpoint), so an orphaned
 * avatar object would never be cleaned up any other way. The storage delete is wrapped in a
 * try/catch: a stricter {@code @TransactionalEventListener(AFTER_COMMIT)} would be defensible,
 * but plain {@code @EventListener} matches every other listener in this module ({@link
 * AuthPrincipalCacheEvictionListener}), and letting a MinIO outage propagate from here would roll
 * back {@code deleteUser}'s transaction and block deactivating a user entirely - an availability
 * bug traded for a storage-hygiene nicety that isn't worth it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class AvatarCleanupListener {

  private final UserRepository userRepository;
  private final ObjectStorage objectStorage;

  @EventListener
  void onUserDeactivated(UserDeactivatedEvent event) {
    userRepository
        .findById(event.userId())
        .filter(user -> user.getAvatarObjectKey() != null)
        .ifPresent(
            user -> {
              String key = user.getAvatarObjectKey();
              user.setAvatarObjectKey(null);
              userRepository.save(user);
              try {
                objectStorage.delete(key);
              } catch (RuntimeException e) {
                log.warn(
                    "Failed to delete avatar object {} for deactivated user {}; leaving it"
                        + " orphaned",
                    key,
                    event.userId(),
                    e);
              }
            });
  }
}
