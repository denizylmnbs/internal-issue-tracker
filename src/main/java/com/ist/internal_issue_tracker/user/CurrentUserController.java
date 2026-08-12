package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Who am I", answered from the token alone. A client needs it because {@code LoginResponse}
 * carries only the access token and the token's only claim is the user's id - so without this a
 * caller knows an id and neither a name nor, more to the point, a role.
 *
 * <p>It sits under {@code /api/auth} because that is the conversation it belongs to, but lives in
 * the {@code user} module rather than in {@code auth}, following {@code UserTeamsController} and
 * {@code UserProjectsController}: the URL says nothing about which module owns the handler. The
 * alternative was to have {@code auth} return a {@link UserResponse}, which would have meant a
 * dependency on {@code user} and a failing {@code ModularityTests} - {@code auth} is declared with
 * {@code allowedDependencies = "shared"} precisely so it stays a login module and not a user one.
 *
 * <p>No authorization rule of its own: the principal <em>is</em> the subject, so there is nothing
 * to compare it against and {@code anyRequest().authenticated()} already says everything needed.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class CurrentUserController {

  private final UserService userService;

  /**
   * Read fresh rather than assembled from the principal. {@link AuthenticatedUser} carries only the
   * id and the role, and the role it carries was itself resolved from the database this request -
   * so going back for the whole row costs the one lookup that was going to happen anyway and
   * returns the same {@link UserResponse} shape as {@code GET /api/users/{id}}, rather than a
   * second, nearly-identical record a client would have to special-case.
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
      @AuthenticationPrincipal AuthenticatedUser caller) {
    UserResponse userResponse = userService.getUserById(caller.getId());

    return ResponseEntity.ok(ApiResponse.ok(userResponse));
  }
}
