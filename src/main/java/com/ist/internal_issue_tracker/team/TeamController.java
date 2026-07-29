package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.team.dto.ChangeLeaderRequest;
import com.ist.internal_issue_tracker.team.dto.TeamCreateRequest;
import com.ist.internal_issue_tracker.team.dto.TeamResponse;
import com.ist.internal_issue_tracker.team.dto.TeamUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {
  private final TeamService teamService;

  @PostMapping
  public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
      @Valid @RequestBody TeamCreateRequest request) {

    TeamResponse teamResponse = teamService.createTeam(request);

    return ResponseEntity.created(URI.create("/api/teams/" + teamResponse.id()))
        .body(ApiResponse.ok(teamResponse));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable Integer id) {
    TeamResponse teamResponse = teamService.getTeamById(id);

    return ResponseEntity.ok(ApiResponse.ok(teamResponse));
  }

  /**
   * Covers "all teams", "teams matching a name/field" and "teams led by someone" - every filter is
   * optional, so an unfiltered call lists everything.
   */
  @GetMapping
  public ResponseEntity<ApiResponse<PagedResponse<TeamResponse>>> getAllTeams(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) TeamField field,
      @RequestParam(required = false) Integer leaderId,
      Pageable pageable) {
    PagedResponse<TeamResponse> teamResponse =
        teamService.getAllTeams(name, field, leaderId, pageable);

    return ResponseEntity.ok(ApiResponse.ok(teamResponse));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
      @PathVariable Integer id, @Valid @RequestBody TeamUpdateRequest request) {
    TeamResponse teamResponse = teamService.updateTeam(id, request);

    return ResponseEntity.ok(ApiResponse.ok(teamResponse));
  }

  @PatchMapping("/{id}/leader")
  public ResponseEntity<ApiResponse<TeamResponse>> changeLeader(
      @PathVariable Integer id, @Valid @RequestBody ChangeLeaderRequest request) {
    TeamResponse teamResponse = teamService.changeLeader(id, request);

    return ResponseEntity.ok(ApiResponse.ok(teamResponse));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable Integer id) {
    teamService.deleteTeam(id);

    return ResponseEntity.ok(ApiResponse.ok());
  }
}
