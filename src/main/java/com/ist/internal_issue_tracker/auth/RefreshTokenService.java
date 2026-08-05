package com.ist.internal_issue_tracker.auth;

import com.ist.internal_issue_tracker.auth.exception.AuthErrorCode;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Server-side half of the access/refresh pair. Refresh tokens are opaque random strings, not JWTs
 * - unlike the access token there is nothing to verify offline, the whole point is that Redis is
 * the source of truth and a token stops working the moment its entry is gone. Only the SHA-256 hash
 * of a token is ever stored, so a Redis dump or an over-broad access grant on that store does not
 * itself hand out usable tokens, the same reasoning as hashing passwords rather than storing them.
 *
 * <p>Two key families cover the two things callers need to do: {@code refresh:token:<hash>} maps a
 * token to the user it belongs to (for validating/consuming one token), and {@code
 * refresh:user:<userId>} is a set of that user's live token hashes (for revoking all of them at
 * once - see {@link #revokeAllForUser}, used when a password changes, a role changes, or an account
 * is deactivated).
 */
@Service
public class RefreshTokenService {

  private static final String TOKEN_KEY_PREFIX = "refresh:token:";
  private static final String USER_KEY_PREFIX = "refresh:user:";
  private static final int TOKEN_BYTE_LENGTH = 32;

  private final StringRedisTemplate redisTemplate;
  private final Duration refreshExpiration;
  private final SecureRandom secureRandom = new SecureRandom();

  public RefreshTokenService(
      StringRedisTemplate redisTemplate,
      @Value("${jwt.refresh-expiration}") long refreshExpirationMillis) {
    this.redisTemplate = redisTemplate;
    this.refreshExpiration = Duration.ofMillis(refreshExpirationMillis);
  }

  /** Issues a new refresh token for the user and stores it in Redis with the configured TTL. */
  public String issue(Integer userId) {
    String token = generateToken();
    String hash = hash(token);

    redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + hash, userId.toString(), refreshExpiration);

    String userKey = USER_KEY_PREFIX + userId;
    redisTemplate.opsForSet().add(userKey, hash);
    redisTemplate.expire(userKey, refreshExpiration);

    return token;
  }

  /**
   * Consumes a refresh token and issues its replacement in one call - rotation, not just
   * validation, so a caller never walks away with a still-live old token. The lookup is destructive
   * (GETDEL) so it also doubles as reuse detection: if the same token is ever presented twice, the
   * second caller finds the key already gone and gets {@link AuthErrorCode#INVALID_REFRESH_TOKEN}
   * instead of a fresh pair, whether that second presentation is a race or an attacker replaying a
   * stolen token.
   */
  public RotationResult rotate(String token) {
    String hash = hash(token);
    String userIdRaw = redisTemplate.opsForValue().getAndDelete(TOKEN_KEY_PREFIX + hash);

    if (userIdRaw == null) {
      throw new AppException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    Integer userId = Integer.valueOf(userIdRaw);
    redisTemplate.opsForSet().remove(USER_KEY_PREFIX + userId, hash);

    return new RotationResult(userId, issue(userId));
  }

  /** The caller needs {@code userId} back too - it is what lets it mint the new access token. */
  public record RotationResult(Integer userId, String refreshToken) {}

  /** Revokes a single refresh token, e.g. on logout. A token that is already gone is a no-op. */
  public void revoke(String token) {
    String hash = hash(token);
    String userIdRaw = redisTemplate.opsForValue().getAndDelete(TOKEN_KEY_PREFIX + hash);

    if (userIdRaw != null) {
      redisTemplate.opsForSet().remove(USER_KEY_PREFIX + Integer.valueOf(userIdRaw), hash);
    }
  }

  /**
   * Revokes every refresh token belonging to a user, so a session issued before a password change,
   * a role change, or a deactivation cannot keep renewing itself past that change.
   */
  public void revokeAllForUser(Integer userId) {
    String userKey = USER_KEY_PREFIX + userId;
    Set<String> hashes = redisTemplate.opsForSet().members(userKey);

    if (hashes != null && !hashes.isEmpty()) {
      hashes.forEach(hash -> redisTemplate.delete(TOKEN_KEY_PREFIX + hash));
    }
    redisTemplate.delete(userKey);
  }

  private String generateToken() {
    byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }
}
