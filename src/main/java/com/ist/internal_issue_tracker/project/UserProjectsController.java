package com.ist.internal_issue_tracker.project;

import com.ist.internal_issue_tracker.project.dto.UserProjectMembershipResponse;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The user-centric view of project work, the counterpart of {@code UserTeamsController}. It sits
 * under {@code /api/users} because that is the resource being read, but lives in the project module
 * - the URL says nothing about which module owns the handler.
 *
 * <p>The path variable is named {@code id} to match the rest of the {@code /api/users} namespace, so
 * a rule like {@code SecurityConfig}'s {@code selfOrAdmin} would apply as-is if this listing is ever
 * narrowed; today it is readable by any authenticated caller through {@code anyRequest}.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProjectsController {
  private final ProjectMemberService projectMemberService;

  @GetMapping("/{id}/projects")
  public ResponseEntity<ApiResponse<PagedResponse<UserProjectMembershipResponse>>>
      getProjectsByUserId(@PathVariable Integer id, Pageable pageable) {
    return ResponseEntity.ok(
        ApiResponse.ok(projectMemberService.getProjectsByUserId(id, pageable)));
  }
}
