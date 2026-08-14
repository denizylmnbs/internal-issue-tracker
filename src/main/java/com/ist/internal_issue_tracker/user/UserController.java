package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.exception.CommonErrorCode;
import com.ist.internal_issue_tracker.shared.ratelimit.RateLimiterService;
import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.user.dto.*;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final UserAvatarService userAvatarService;
  private final RateLimiterService rateLimiterService;

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<UserResponse>> createUser(
      @Valid @RequestBody UserCreateRequest request) {

    // aynı e-postayla tekrar tekrar hesap açma denemesini IP'den bağımsız olarak sınırlar
    if (!rateLimiterService.tryConsume(
        "account:register:" + request.email(), RateLimiterService.perAccount())) {
      throw new AppException(CommonErrorCode.RATE_LIMITED);
    }

    UserResponse userResponse = userService.createUser(request);

    return ResponseEntity.created(URI.create("/api/users/" + userResponse.id()))
        .body(ApiResponse.ok(userResponse));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Integer id) {
    UserResponse userResponse = userService.getUserById(id);

    return ResponseEntity.ok(ApiResponse.ok(userResponse));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String surname,
      Pageable pageable) {
    PagedResponse<UserResponse> userResponse = userService.getAllUsers(name, surname, pageable);

    return ResponseEntity.ok(ApiResponse.ok(userResponse));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> updateUser(
      @PathVariable Integer id, @Valid @RequestBody UserUpdateRequest request) {
    UserResponse userResponse = userService.updateUser(id, request);

    return ResponseEntity.ok(ApiResponse.ok(userResponse));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer id) {
    userService.deleteUser(id);

    return ResponseEntity.ok(ApiResponse.ok());
  }

  @PatchMapping("/{id}/password")
  public ResponseEntity<ApiResponse<UserResponse>> changePassword(
      @PathVariable Integer id, @Valid @RequestBody ChangePasswordRequest request) {
    UserResponse userResponse = userService.changePassword(id, request);

    return ResponseEntity.ok(ApiResponse.ok(userResponse));
  }

  @PostMapping("/{id}/reset-password")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
      @PathVariable Integer id, @Valid @RequestBody ResetPasswordRequest request) {
    userService.resetPassword(id, request);

    return ResponseEntity.ok(ApiResponse.ok());
  }

  // consumes = MULTIPART_FORM_DATA_VALUE turns a wrong content type into a clean framework 415
  // instead of a confusing bind failure. Part name "file" must match uploadAvatar in the
  // frontend's users.ts endpoint file exactly.
  @PutMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
      @PathVariable Integer id, @RequestPart("file") MultipartFile file) {
    UserResponse userResponse = userAvatarService.replaceAvatar(id, file);

    return ResponseEntity.ok(ApiResponse.ok(userResponse));
  }

  // Returns the refreshed UserResponse rather than Void: unlike DELETE /{id}, the user still
  // exists afterwards and the caller wants its new state (avatarUrl now null) in the same round
  // trip.
  @DeleteMapping("/{id}/avatar")
  public ResponseEntity<ApiResponse<UserResponse>> deleteAvatar(@PathVariable Integer id) {
    UserResponse userResponse = userAvatarService.removeAvatar(id);

    return ResponseEntity.ok(ApiResponse.ok(userResponse));
  }

  @PatchMapping("/{id}/role")
  public ResponseEntity<ApiResponse<UserResponse>> changeRole(
      @AuthenticationPrincipal AuthenticatedUser caller,
      @PathVariable Integer id,
      @Valid @RequestBody RoleChangeRequest request) {
    UserResponse userResponse = userService.changeRole(id, request, caller);

    return ResponseEntity.ok(ApiResponse.ok(userResponse));
  }
}
