package com.ist.internal_issue_tracker.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class UserAvatarServiceTest {

  private static final byte[] PNG_BYTES = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };

  /** What the normalizer hands back - deliberately different bytes than went in. */
  private static final byte[] NORMALIZED_BYTES = {1, 2, 3, 4};

  private static final ImageNormalizer.NormalizedImage NORMALIZED =
      new ImageNormalizer.NormalizedImage(NORMALIZED_BYTES, "image/png");

  @Mock private UserRepository userRepository;
  @Mock private UserMapper userMapper;
  @Mock private ObjectStorage objectStorage;
  @Mock private ImageContentTypeDetector contentTypeDetector;
  @Mock private ImageNormalizer imageNormalizer;
  @Mock private VirusScanner virusScanner;

  private UserAvatarService userAvatarService;

  // Built by hand rather than with @InjectMocks: the scanner is an Optional dependency and half
  // these tests need it absent, which is the whole point of modelling "disabled" as an empty
  // Optional instead of a no-op bean.
  @BeforeEach
  void setUp() {
    userAvatarService = withScanner(Optional.of(virusScanner));
  }

  private UserAvatarService withScanner(Optional<VirusScanner> scanner) {
    return new UserAvatarService(
        userRepository, userMapper, objectStorage, contentTypeDetector, imageNormalizer, scanner);
  }

  private static MockMultipartFile pngFile() {
    return new MockMultipartFile("file", "avatar.png", "image/gif", PNG_BYTES);
  }

  /** The common "valid PNG, clean scan, normalizes fine" setup. */
  private void givenAcceptableUpload() {
    when(contentTypeDetector.detect(PNG_BYTES)).thenReturn(Optional.of("image/png"));
    when(virusScanner.scan(PNG_BYTES)).thenReturn(VirusScanner.ScanResult.clean());
    when(imageNormalizer.normalize(eq(PNG_BYTES), anyInt())).thenReturn(NORMALIZED);
  }

  @Test
  void replaceAvatar_storesTheNormalizedBytesAsPng_notTheUploadedOnes() {
    // Two guarantees in one assertion. The part declares "image/gif" and that claim must never
    // reach ObjectStorage; and what gets stored is the normalizer's output, so an attacker's
    // original bytes - whatever was hiding past the PNG header - are never the thing persisted.
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    givenAcceptableUpload();
    when(objectStorage.put(eq("avatars/1"), eq(NORMALIZED_BYTES), eq("image/png")))
        .thenReturn("avatars/1/new-key");
    when(userRepository.save(user)).thenReturn(user);

    userAvatarService.replaceAvatar(1, pngFile());

    verify(objectStorage).put("avatars/1", NORMALIZED_BYTES, "image/png");
    verify(objectStorage, never()).put(any(), eq(PNG_BYTES), any());
    assertThat(user.getAvatarObjectKey()).isEqualTo("avatars/1/new-key");
  }

  @Test
  void replaceAvatar_scansTheOriginalBytesBeforeNormalizingThem() {
    // Order invariant. Normalizing first would hand the scanner a file this service has already
    // sanitized: the payload would be gone, the verdict would always be clean, and the control
    // would be decorative.
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    givenAcceptableUpload();
    when(objectStorage.put(any(), any(), any())).thenReturn("avatars/1/new-key");
    when(userRepository.save(user)).thenReturn(user);

    userAvatarService.replaceAvatar(1, pngFile());

    InOrder order = inOrder(virusScanner, imageNormalizer, objectStorage);
    order.verify(virusScanner).scan(PNG_BYTES);
    order.verify(imageNormalizer).normalize(eq(PNG_BYTES), anyInt());
    order.verify(objectStorage).put(any(), any(), any());
  }

  @Test
  void replaceAvatar_rejectsAndStoresNothing_whenTheScannerReportsMalware() {
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(contentTypeDetector.detect(PNG_BYTES)).thenReturn(Optional.of("image/png"));
    when(virusScanner.scan(PNG_BYTES))
        .thenReturn(VirusScanner.ScanResult.infected("Eicar-Test-Signature"));

    assertThatThrownBy(() -> userAvatarService.replaceAvatar(1, pngFile()))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(CommonErrorCode.MALWARE_DETECTED);

    verify(objectStorage, never()).put(any(), any(), any());
    verify(imageNormalizer, never()).normalize(any(), anyInt());
  }

  @Test
  void replaceAvatar_failsClosed_whenTheScannerIsUnavailable() {
    // The single most important test here: degrading to "store it anyway" under exactly the
    // conditions an attacker would try to create is what makes a scanner pointless.
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(contentTypeDetector.detect(PNG_BYTES)).thenReturn(Optional.of("image/png"));
    when(virusScanner.scan(PNG_BYTES)).thenReturn(VirusScanner.ScanResult.unavailable());

    assertThatThrownBy(() -> userAvatarService.replaceAvatar(1, pngFile()))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(CommonErrorCode.SERVICE_UNAVAILABLE);

    verify(objectStorage, never()).put(any(), any(), any());
  }

  @Test
  void replaceAvatar_stillNormalizes_whenScanningIsDisabled() {
    // app.storage.av.enabled=false removes the bean entirely; everything else must still apply.
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(contentTypeDetector.detect(PNG_BYTES)).thenReturn(Optional.of("image/png"));
    when(imageNormalizer.normalize(eq(PNG_BYTES), anyInt())).thenReturn(NORMALIZED);
    when(objectStorage.put(any(), any(), any())).thenReturn("avatars/1/new-key");
    when(userRepository.save(user)).thenReturn(user);

    withScanner(Optional.empty()).replaceAvatar(1, pngFile());

    verify(objectStorage).put("avatars/1", NORMALIZED_BYTES, "image/png");
    verify(virusScanner, never()).scan(any());
  }

  @Test
  void replaceAvatar_throwsUnsupportedMediaType_whenTheImageCannotBeDecoded() {
    // A valid signature followed by garbage: the magic-byte gate passes it, the decode does not.
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(contentTypeDetector.detect(PNG_BYTES)).thenReturn(Optional.of("image/png"));
    when(virusScanner.scan(PNG_BYTES)).thenReturn(VirusScanner.ScanResult.clean());
    when(imageNormalizer.normalize(eq(PNG_BYTES), anyInt()))
        .thenThrow(
            new ImageProcessingException(
                ImageProcessingException.Reason.UNREADABLE, "not decodable"));

    assertThatThrownBy(() -> userAvatarService.replaceAvatar(1, pngFile()))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);

    verify(objectStorage, never()).put(any(), any(), any());
  }

  @Test
  void replaceAvatar_throwsPayloadTooLarge_whenTheImageDecodesToTooManyPixels() {
    // Within the 2MB byte cap but a decompression bomb by pixel count - the case the byte cap
    // structurally cannot catch.
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(contentTypeDetector.detect(PNG_BYTES)).thenReturn(Optional.of("image/png"));
    when(virusScanner.scan(PNG_BYTES)).thenReturn(VirusScanner.ScanResult.clean());
    when(imageNormalizer.normalize(eq(PNG_BYTES), anyInt()))
        .thenThrow(
            new ImageProcessingException(
                ImageProcessingException.Reason.TOO_LARGE, "30000x30000 exceeds the maximum"));

    assertThatThrownBy(() -> userAvatarService.replaceAvatar(1, pngFile()))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(CommonErrorCode.PAYLOAD_TOO_LARGE);

    verify(objectStorage, never()).put(any(), any(), any());
  }

  @Test
  void replaceAvatar_savesBeforeDeletingThePreviousObject_whenOneExisted() {
    // The invariant this guards: if save fails/rolls back, the previous object must still exist.
    // Deleting it first would risk stranding the row pointing at nothing.
    User user = new User();
    user.setAvatarObjectKey("avatars/1/old-key");
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    givenAcceptableUpload();
    when(objectStorage.put(any(), any(), any())).thenReturn("avatars/1/new-key");
    when(userRepository.save(user)).thenReturn(user);

    userAvatarService.replaceAvatar(1, pngFile());

    InOrder order = inOrder(userRepository, objectStorage);
    order.verify(userRepository).save(user);
    order.verify(objectStorage).delete("avatars/1/old-key");
  }

  @Test
  void replaceAvatar_neverCallsDelete_whenUserHadNoPreviousAvatar() {
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    givenAcceptableUpload();
    when(objectStorage.put(any(), any(), any())).thenReturn("avatars/1/new-key");
    when(userRepository.save(user)).thenReturn(user);

    userAvatarService.replaceAvatar(1, pngFile());

    verify(objectStorage, never()).delete(any());
  }

  @Test
  void replaceAvatar_stillReturns_whenDeletingThePreviousObjectFails() {
    // Best-effort: a storage hiccup on cleanup of the old object must not fail the whole
    // operation - the new avatar is already saved and correct.
    User user = new User();
    user.setAvatarObjectKey("avatars/1/old-key");
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    givenAcceptableUpload();
    when(objectStorage.put(any(), any(), any())).thenReturn("avatars/1/new-key");
    when(userRepository.save(user)).thenReturn(user);
    UserResponse response = new UserResponse(1, "Ada", "L", "a@ist.com", null, true, null, null);
    when(userMapper.toResponse(user)).thenReturn(response);
    doThrow(new RuntimeException("minio down")).when(objectStorage).delete("avatars/1/old-key");

    assertThat(userAvatarService.replaceAvatar(1, pngFile())).isEqualTo(response);
  }

  @Test
  void replaceAvatar_throwsPayloadTooLarge_whenFileExceedsTheCap() {
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    byte[] oversized = new byte[3 * 1024 * 1024];
    MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);

    assertThatThrownBy(() -> userAvatarService.replaceAvatar(1, file))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(CommonErrorCode.PAYLOAD_TOO_LARGE);

    verify(objectStorage, never()).put(any(), any(), any());
  }

  @Test
  void replaceAvatar_throwsUnsupportedMediaType_whenBytesDoNotMatchAnyKnownImageFormat() {
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    byte[] notAnImage = "hello".getBytes();
    when(contentTypeDetector.detect(notAnImage)).thenReturn(Optional.empty());
    MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", notAnImage);

    assertThatThrownBy(() -> userAvatarService.replaceAvatar(1, file))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);

    verify(objectStorage, never()).put(any(), any(), any());
  }

  @Test
  void replaceAvatar_rejectsWebp_eventhoughTheDetectorRecognizesIt() {
    // The detector stays policy-free and still identifies WebP; the allow-list is what dropped it,
    // because stock ImageIO cannot decode WebP and decoding is now mandatory.
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(contentTypeDetector.detect(PNG_BYTES)).thenReturn(Optional.of("image/webp"));

    assertThatThrownBy(() -> userAvatarService.replaceAvatar(1, pngFile()))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);

    verify(virusScanner, never()).scan(any());
    verify(objectStorage, never()).put(any(), any(), any());
  }

  @Test
  void replaceAvatar_throwsValidationFailed_whenFileIsEmpty() {
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    MockMultipartFile empty = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);

    assertThatThrownBy(() -> userAvatarService.replaceAvatar(1, empty))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).errorCode())
        .isEqualTo(CommonErrorCode.VALIDATION_FAILED);

    verify(objectStorage, never()).put(any(), any(), any());
  }

  @Test
  void replaceAvatar_throwsResourceNotFoundException_whenUserDoesNotExist() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userAvatarService.replaceAvatar(1, pngFile()))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(objectStorage, never()).put(any(), any(), any());
  }

  @Test
  void removeAvatar_clearsTheKeyAndDeletesTheObject_whenOneExists() {
    User user = new User();
    user.setAvatarObjectKey("avatars/1/old-key");
    when(userRepository.findById(1)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    userAvatarService.removeAvatar(1);

    assertThat(user.getAvatarObjectKey()).isNull();
    verify(objectStorage).delete("avatars/1/old-key");
  }

  @Test
  void removeAvatar_isIdempotent_whenUserAlreadyHasNoAvatar() {
    User user = new User();
    when(userRepository.findById(1)).thenReturn(Optional.of(user));

    userAvatarService.removeAvatar(1);

    verify(userRepository, never()).save(any());
    verify(objectStorage, never()).delete(any());
  }

  @Test
  void removeAvatar_throwsResourceNotFoundException_whenUserDoesNotExist() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userAvatarService.removeAvatar(1))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
