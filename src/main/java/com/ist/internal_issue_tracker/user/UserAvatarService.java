package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.exception.CommonErrorCode;
import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;
import com.ist.internal_issue_tracker.shared.storage.ImageContentTypeDetector;
import com.ist.internal_issue_tracker.shared.storage.ImageNormalizer;
import com.ist.internal_issue_tracker.shared.storage.ImageProcessingException;
import com.ist.internal_issue_tracker.shared.storage.ObjectStorage;
import com.ist.internal_issue_tracker.shared.storage.VirusScanner;
import com.ist.internal_issue_tracker.user.dto.UserResponse;
import com.ist.internal_issue_tracker.user.mapper.UserMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Kept separate from {@link UserService} rather than folded into it: {@code UserService} is
 * already credentials/roles/lifecycle, and avatar handling brings an entirely different
 * dependency set (object storage, image sniffing) that has nothing to do with those concerns.
 * Still inside the {@code user} module, so nothing about module boundaries changes.
 *
 * <p>Neither {@link #replaceAvatar} nor {@link #removeAvatar} is {@code @Transactional} - each is
 * a single write, like {@link UserService#changePassword}. In {@link #replaceAvatar} specifically,
 * the previous object is deleted <em>after</em> the new key is saved, and that ordering is the
 * whole point: deleting first and then having the save roll back would leave the row pointing at
 * an object that no longer exists - a permanently broken avatar. Deleting after means the worst
 * case is one orphaned object in storage. Orphan over broken link. Making this method
 * {@code @Transactional} would make that worse, not better: a post-save rollback would then strand
 * a deleted object under a key the database still believes is live.
 *
 * <p>{@link #replaceAvatar} runs four checks in a fixed order, and each position is load-bearing:
 *
 * <ol>
 *   <li><b>Size</b>, then the <b>magic-byte gate</b> - both cheap and local. They reject obviously
 *       wrong input before spending a network round trip on the malware scanner or CPU on a decode.
 *   <li><b>Malware scan</b>, on the <em>original</em> bytes. Scanning after normalization would
 *       scan a file this service just sanitized: the payload is already gone, so a clean verdict
 *       would prove nothing and the "user X tried to upload this" log line would never happen.
 *   <li><b>Normalization</b> last, because it is the most expensive step and everything above has
 *       already narrowed the input down to something worth decoding.
 * </ol>
 *
 * <p>An unreachable scanner fails <em>closed</em> - the upload is rejected rather than stored
 * unscanned. That is the whole reason the scan exists; degrading to "store it anyway" under exactly
 * the conditions an attacker would try to create makes the control decorative. Scanning can still
 * be switched off wholesale via {@code app.storage.av.enabled}, which is an explicit, logged
 * operator decision rather than something a dropped connection can cause.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAvatarService {

  /**
   * {@code image/svg+xml} is deliberately not here: an SVG served back from the object-storage
   * origin - which is exactly what happens when a browser opens a presigned URL - can carry
   * inline {@code <script>}, making it an XSS vector rather than an image format.
   */
  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg");

  private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;

  /** Longest edge of the stored avatar. Every upload is scaled down to this; none is scaled up. */
  private static final int AVATAR_SIZE = 512;

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final ObjectStorage objectStorage;
  private final ImageContentTypeDetector contentTypeDetector;
  private final ImageNormalizer imageNormalizer;

  /** Empty when {@code app.storage.av.enabled} is false - see the class Javadoc. */
  private final Optional<VirusScanner> virusScanner;

  public UserResponse replaceAvatar(Integer userId, MultipartFile file) {
    User user = requireUser(userId);
    validateSize(file);
    byte[] content = readBytes(file);
    requireSupportedImage(content);
    requireMalwareFree(userId, content);
    ImageNormalizer.NormalizedImage normalized = normalize(content);

    String previousKey = user.getAvatarObjectKey();
    String newKey =
        objectStorage.put("avatars/" + userId, normalized.content(), normalized.contentType());
    user.setAvatarObjectKey(newKey);
    User saved = userRepository.save(user);

    if (previousKey != null) {
      deleteBestEffort(previousKey);
    }

    return userMapper.toResponse(saved);
  }

  public UserResponse removeAvatar(Integer userId) {
    User user = requireUser(userId);
    String previousKey = user.getAvatarObjectKey();

    if (previousKey == null) {
      return userMapper.toResponse(user);
    }

    user.setAvatarObjectKey(null);
    User saved = userRepository.save(user);
    deleteBestEffort(previousKey);

    return userMapper.toResponse(saved);
  }

  private User requireUser(Integer userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
  }

  private void validateSize(MultipartFile file) {
    if (file.isEmpty()) {
      throw new AppException(CommonErrorCode.VALIDATION_FAILED, "The uploaded file is empty");
    }
    if (file.getSize() > MAX_AVATAR_BYTES) {
      throw new AppException(CommonErrorCode.PAYLOAD_TOO_LARGE);
    }
  }

  private byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read uploaded avatar", e);
    }
  }

  /**
   * The declared {@code Content-Type} on {@code file} is never consulted here or anywhere
   * downstream - only the sniffed value from {@link ImageContentTypeDetector} is checked against
   * the allow-list, so a client claiming {@code image/gif} for PNG bytes cannot spoof its way past
   * this check.
   *
   * <p>The sniffed value is deliberately <em>not</em> returned any more: it used to be what got
   * stored, but {@link ImageNormalizer} re-encodes every upload to PNG, so the stored type comes
   * from the normalizer's output rather than from anything about the input. This is now only a
   * gate. {@code image/webp} is no longer on the allow-list - stock ImageIO cannot decode it, and
   * decoding is a hard requirement for the normalization step below.
   */
  private void requireSupportedImage(byte[] content) {
    Optional<String> detected = contentTypeDetector.detect(content);
    if (detected.isEmpty() || !ALLOWED_CONTENT_TYPES.contains(detected.get())) {
      throw new AppException(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }
  }

  private void requireMalwareFree(Integer userId, byte[] content) {
    if (virusScanner.isEmpty()) {
      return;
    }
    VirusScanner.ScanResult result = virusScanner.get().scan(content);
    switch (result.status()) {
      case INFECTED -> {
        // The signature name goes to the log, never to the response: telling a client which
        // definition matched is precisely the feedback needed to iterate around it.
        log.warn(
            "Rejected avatar upload for user {}: malware signature {}", userId, result.signature());
        throw new AppException(CommonErrorCode.MALWARE_DETECTED);
      }
      case UNAVAILABLE -> {
        log.error("Rejecting avatar upload for user {}: malware scanner unavailable", userId);
        throw new AppException(CommonErrorCode.SERVICE_UNAVAILABLE);
      }
      case CLEAN -> {
        // fall through
      }
    }
  }

  private ImageNormalizer.NormalizedImage normalize(byte[] content) {
    try {
      return imageNormalizer.normalize(content, AVATAR_SIZE);
    } catch (ImageProcessingException e) {
      throw switch (e.reason()) {
        case TOO_LARGE -> new AppException(CommonErrorCode.PAYLOAD_TOO_LARGE, e.getMessage());
        case UNREADABLE -> new AppException(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
      };
    }
  }

  private void deleteBestEffort(String objectKey) {
    try {
      objectStorage.delete(objectKey);
    } catch (RuntimeException e) {
      log.warn("Failed to delete stale avatar object {}; leaving it orphaned", objectKey, e);
    }
  }
}
