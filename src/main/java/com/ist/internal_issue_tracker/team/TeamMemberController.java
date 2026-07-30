package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.team.dto.TeamMemberCreateRequest;
import com.ist.internal_issue_tracker.team.dto.TeamMemberResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Membership is addressed under the team that owns it. The path variable is named {@code id} rather
 * than {@code teamId} on purpose: {@code SecurityConfig}'s authorization managers read it through
 * {@code RequestAuthorizationContext#getVariables()}, which looks the name up literally.
 */
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamMemberController {
  private final TeamMemberService teamMemberService;

  @PostMapping("/{id}/members")
  public ResponseEntity<ApiResponse<TeamMemberResponse>> createTeamMember(
      @PathVariable Integer id, @Valid @RequestBody TeamMemberCreateRequest request) {
    TeamMemberResponse teamMemberResponse = teamMemberService.createTeamMember(id, request);

    return ResponseEntity.created(
            URI.create("/api/teams/" + id + "/members/" + teamMemberResponse.userId()))
        .body(ApiResponse.ok(teamMemberResponse));
  }

  @GetMapping("/{id}/members")
  public ResponseEntity<ApiResponse<PagedResponse<TeamMemberResponse>>> getTeamMembers(
      @PathVariable Integer id, Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(teamMemberService.getTeamMembersByTeamId(id, pageable)));
  }

  /**
   * Every membership in the system, across teams. The literal {@code /members} segment is matched
   * ahead of {@code TeamController}'s {@code /api/teams/{id}}, because Spring's path patterns rank a
   * literal above a variable at the same position.
   */
  @GetMapping("/members")
  public ResponseEntity<ApiResponse<PagedResponse<TeamMemberResponse>>> getAllTeamMembers(
      Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(teamMemberService.getAllTeamMembers(pageable)));
  }

  /**
   * Removing a member is a soft delete, so the membership is addressed by the pair the schema
   * actually keys an active membership on ({@code unique_active_team_membership}) rather than by its
   * surrogate id. Keeping the team in the path is also what lets the existing team-leader
   * authorization rule apply unchanged.
   */
  @DeleteMapping("/{id}/members/{userId}")
  public ResponseEntity<ApiResponse<Void>> removeTeamMember(
      @PathVariable Integer id, @PathVariable Integer userId) {
    teamMemberService.removeTeamMember(id, userId);

    return ResponseEntity.ok(ApiResponse.ok());
  }
}
