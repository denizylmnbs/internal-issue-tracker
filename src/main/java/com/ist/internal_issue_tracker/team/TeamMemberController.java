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
@RequestMapping("/api/teams/{id}/members")
@RequiredArgsConstructor
public class TeamMemberController {
  private final TeamMemberService teamMemberService;

  @PostMapping
  public ResponseEntity<ApiResponse<TeamMemberResponse>> createTeamMember(
      @PathVariable Integer id, @Valid @RequestBody TeamMemberCreateRequest request) {
    TeamMemberResponse teamMemberResponse = teamMemberService.createTeamMember(id, request);

    return ResponseEntity.created(
            URI.create("/api/teams/" + id + "/members/" + teamMemberResponse.id()))
        .body(ApiResponse.ok(teamMemberResponse));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PagedResponse<TeamMemberResponse>>> getTeamMembers(
      @PathVariable Integer id, Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(teamMemberService.getTeamMembers(id, pageable)));
  }
}
