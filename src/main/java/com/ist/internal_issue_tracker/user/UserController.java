package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.user.dto.*;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<UserResponse>> createUser(
      @Valid @RequestBody UserCreateRequest request) {

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

  @PatchMapping("/{id}/role")
  public ResponseEntity<ApiResponse<UserResponse>> changeRole(
          @AuthenticationPrincipal AuthenticatedUser caller,
          @PathVariable Integer id, @Valid @RequestBody RoleChangeRequest request
  ) {
    UserResponse userResponse = userService.changeRole(id, request, caller);

    return ResponseEntity.ok(ApiResponse.ok(userResponse));
  }
}
