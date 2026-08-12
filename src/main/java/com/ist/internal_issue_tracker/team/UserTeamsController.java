package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.team.dto.UserTeamMembershipResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The user-centric view of membership: which teams someone belongs to, rather than who belongs to a
 * team. It sits under {@code /api/users} because that is the resource being read, but lives in the
 * team module - the URL says nothing about which module owns the handler, and keeping it here
 * avoids a dependency from {@code user} on team internals.
 *
 * <p>The path variable is named {@code id} to match the rest of the {@code /api/users} namespace,
 * so a rule like {@code SecurityConfig}'s {@code selfOrAdmin} would apply as-is if this listing is
 * ever narrowed; today it is readable by any authenticated caller through {@code anyRequest}.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserTeamsController {
  private final TeamMemberService teamMemberService;

  @GetMapping("/{id}/teams")
  public ResponseEntity<ApiResponse<PagedResponse<UserTeamMembershipResponse>>> getTeamsByUserId(
      @PathVariable Integer id, Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(teamMemberService.getTeamsByUserId(id, pageable)));
  }
}
