package com.ist.internal_issue_tracker.shared.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ist.internal_issue_tracker.project.ProjectController;
import com.ist.internal_issue_tracker.project.ProjectMemberController;
import com.ist.internal_issue_tracker.project.ProjectMemberService;
import com.ist.internal_issue_tracker.project.ProjectService;
import com.ist.internal_issue_tracker.project.ProjectTeamController;
import com.ist.internal_issue_tracker.project.ProjectTeamService;
import com.ist.internal_issue_tracker.project.UserProjectsController;
import com.ist.internal_issue_tracker.project.dto.ProjectMemberResponse;
import com.ist.internal_issue_tracker.project.dto.ProjectTeamResponse;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import com.ist.internal_issue_tracker.team.TeamController;
import com.ist.internal_issue_tracker.team.TeamMemberController;
import com.ist.internal_issue_tracker.team.TeamMemberService;
import com.ist.internal_issue_tracker.team.TeamService;
import com.ist.internal_issue_tracker.team.UserTeamsController;
import com.ist.internal_issue_tracker.team.dto.TeamMemberResponse;
import com.ist.internal_issue_tracker.user.UserController;
import com.ist.internal_issue_tracker.user.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Exercises the authorization rules through the real filter chain. The JWT filter is not involved -
 * requests carry a pre-built {@code Authentication} identical in shape to the one {@link
 * JwtAuthenticationFilter} produces: an {@link AuthenticatedUser} principal plus the caller's own
 * role, and only that role.
 *
 */
@WebMvcTest(
    controllers = {
      UserController.class,
      TeamController.class,
      TeamMemberController.class,
      UserTeamsController.class,
      ProjectController.class,
      ProjectMemberController.class,
      ProjectTeamController.class,
      UserProjectsController.class
    })
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class SecurityConfigTest {

  private static final String RESET_PASSWORD_BODY = "{\"newPassword\":\"password123\"}";
  private static final String CHANGE_PASSWORD_BODY =
      "{\"currentPassword\":\"password123\",\"newPassword\":\"password456\"}";
  private static final String CHANGE_ROLE_BODY = "{\"newRole\":\"EDITOR\"}";
  private static final String ADD_MEMBER_BODY = "{\"userId\":7}";
  private static final String UPDATE_PROJECT_BODY =
      "{\"name\":\"Apollo\",\"description\":\"x\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-06-01\"}";
  private static final String CHANGE_STATUS_BODY = "{\"status\":\"ACTIVE\"}";
  private static final String CHANGE_LEADER_BODY = "{\"leaderId\":7}";
  private static final String ADD_PROJECT_TEAM_BODY = "{\"teamId\":3}";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;
  @MockitoBean private TeamService teamService;
  @MockitoBean private TeamMemberService teamMemberService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private AuthenticatedUserLookup authenticatedUserLookup;

  @MockitoBean private ProjectService projectService;
  @MockitoBean private ProjectMemberService projectMemberService;
  @MockitoBean private ProjectTeamService projectTeamService;

  /** {@code securityFilterChain} takes the ports directly, so the slice has to supply them. */
  @MockitoBean private TeamLookup teamLookup;

  @MockitoBean private ProjectLookup projectLookup;

  /** Mirrors {@link JwtAuthenticationFilter}: the principal carries exactly one authority. */
  private static RequestPostProcessor as(Integer userId, Role role) {
    return authentication(
        new UsernamePasswordAuthenticationToken(
            new AuthenticatedUser(userId, role),
            null,
            List.of(new SimpleGrantedAuthority(role.authority()))));
  }

  @Test
  void anyRequest_returns401_whenUnauthenticated() throws Exception {
    mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @EnumSource(Role.class)
  void anyRequest_isAllowed_forEveryAuthenticatedRole(Role role) throws Exception {
    mockMvc.perform(get("/api/users").with(as(1, role))).andExpect(status().isOk());
  }

  @Test
  void resetPassword_isAllowed_forAdmin() throws Exception {
    mockMvc
        .perform(
            post("/api/users/1/reset-password")
                .with(as(99, Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(RESET_PASSWORD_BODY))
        .andExpect(status().isOk());
  }

  /**
   * The hierarchy only ever grants <em>downwards</em>: an editor outranks a developer but must not
   * reach an admin-only rule.
   */
  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER", "EDITOR"})
  void resetPassword_returns403_forEveryRoleBelowAdmin(Role role) throws Exception {
    mockMvc
        .perform(
            post("/api/users/1/reset-password")
                .with(as(99, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(RESET_PASSWORD_BODY))
        .andExpect(status().isForbidden());
  }

  /**
   * The hierarchy's whole purpose, observed end to end: {@code DELETE /api/teams/{id}} declares only
   * {@code hasRole("EDITOR")}, yet an admin gets through without the rule ever naming {@code ADMIN}.
   * Deleting the {@code EDITOR -> DEVELOPER} link or the {@code RoleHierarchy} bean turns the admin
   * case red.
   */
  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"EDITOR", "ADMIN"})
  void deleteTeam_isAllowed_forEveryRoleFromEditorUp(Role role) throws Exception {
    mockMvc.perform(delete("/api/teams/1").with(as(1, role))).andExpect(status().isOk());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER"})
  void deleteTeam_returns403_forEveryRoleBelowEditor(Role role) throws Exception {
    mockMvc.perform(delete("/api/teams/1").with(as(1, role))).andExpect(status().isForbidden());
  }

  @Test
  void changePassword_isAllowed_forOwner() throws Exception {
    mockMvc
        .perform(
            patch("/api/users/1/password")
                .with(as(1, Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_PASSWORD_BODY))
        .andExpect(status().isOk());
  }

  @Test
  void changePassword_returns403_forAnotherUser() throws Exception {
    mockMvc
        .perform(
            patch("/api/users/2/password")
                .with(as(1, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_PASSWORD_BODY))
        .andExpect(status().isForbidden());
  }

  /**
   * Regression test for the hand-written {@code selfOrAdmin} manager: it reads authorities itself,
   * so it has to expand them through the {@code RoleHierarchy} first. Comparing {@code
   * getAuthorities()} directly would still pass here (admin is the top role) - the case that would
   * break is covered by {@link #changePassword_returns403_forAnotherUser} staying red for any role
   * that is neither owner nor admin.
   */
  @Test
  void changePassword_isAllowed_forAdminActingOnAnotherUser() throws Exception {
    mockMvc
        .perform(
            patch("/api/users/1/password")
                .with(as(99, Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_PASSWORD_BODY))
        .andExpect(status().isOk());
  }

  /**
   * Only the coarse gate is observable here: whether this admin may hand out <em>this</em> role to
   * <em>this</em> user depends on rows the filter chain never reads, and is covered by {@code
   * UserServiceTest}. Getting past the gate is therefore all a 200 proves - {@code UserService} is
   * mocked out.
   */
  @Test
  void changeRole_isAllowed_forAdmin() throws Exception {
    mockMvc
        .perform(
            patch("/api/users/1/role")
                .with(as(99, Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_ROLE_BODY))
        .andExpect(status().isOk());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER", "EDITOR"})
  void changeRole_returns403_forEveryRoleBelowAdmin(Role role) throws Exception {
    mockMvc
        .perform(
            patch("/api/users/1/role")
                .with(as(99, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_ROLE_BODY))
        .andExpect(status().isForbidden());
  }

  private ResultActions addMemberToTeam1(Integer callerId, Role role) throws Exception {
    return mockMvc.perform(
        post("/api/teams/1/members")
            .with(as(callerId, role))
            .contentType(MediaType.APPLICATION_JSON)
            .content(ADD_MEMBER_BODY));
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"EDITOR", "ADMIN"})
  void addTeamMember_isAllowed_forEveryRoleFromEditorUp(Role role) throws Exception {
    when(teamMemberService.createTeamMember(eq(1), any()))
        .thenReturn(new TeamMemberResponse(10, 7, 1, true));

    addMemberToTeam1(99, role).andExpect(status().isCreated());
  }

  /**
   * The reason the team id sits in the path and not in the request body: {@code editorOrTeamLeader}
   * runs before the controller and can only reach path variables, so a body-carried id would leave
   * this leader branch permanently unreachable and silently degrade the rule to {@code
   * hasRole("EDITOR")}.
   */
  @Test
  void addTeamMember_isAllowed_forTheLeaderOfThatTeam() throws Exception {
    when(teamLookup.isLeaderOfTeam(1, 5)).thenReturn(true);
    when(teamMemberService.createTeamMember(eq(1), any()))
        .thenReturn(new TeamMemberResponse(10, 7, 1, true));

    addMemberToTeam1(5, Role.DEVELOPER).andExpect(status().isCreated());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER"})
  void addTeamMember_returns403_forEveryRoleBelowEditorThatDoesNotLeadTheTeam(Role role)
      throws Exception {
    addMemberToTeam1(5, role).andExpect(status().isForbidden());
  }

  /** Reading the roster is not restricted beyond being logged in. */
  @ParameterizedTest
  @EnumSource(Role.class)
  void getTeamMembers_isAllowed_forEveryAuthenticatedRole(Role role) throws Exception {
    mockMvc
        .perform(get("/api/teams/1/members").with(as(1, role)))
        .andExpect(status().isOk());
  }

  /** Removing a member is gated exactly like adding one. */
  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"EDITOR", "ADMIN"})
  void removeTeamMember_isAllowed_forEveryRoleFromEditorUp(Role role) throws Exception {
    mockMvc.perform(delete("/api/teams/1/members/7").with(as(99, role))).andExpect(status().isOk());
  }

  @Test
  void removeTeamMember_isAllowed_forTheLeaderOfThatTeam() throws Exception {
    when(teamLookup.isLeaderOfTeam(1, 5)).thenReturn(true);

    mockMvc
        .perform(delete("/api/teams/1/members/7").with(as(5, Role.DEVELOPER)))
        .andExpect(status().isOk());
  }

  /**
   * The trailing {@code {userId}} must not be mistaken for the team the rule authorizes against:
   * user 7 leads team 7, not team 1, so the request is still refused.
   */
  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER"})
  void removeTeamMember_returns403_forEveryRoleBelowEditorThatDoesNotLeadTheTeam(Role role)
      throws Exception {
    when(teamLookup.isLeaderOfTeam(7, 7)).thenReturn(true);

    mockMvc
        .perform(delete("/api/teams/1/members/7").with(as(7, role)))
        .andExpect(status().isForbidden());
  }

  /**
   * Someone else's team list is readable too - the rule is {@code anyRequest().authenticated()}, not
   * self-or-admin, so the caller's id deliberately differs from the one in the path.
   */
  @ParameterizedTest
  @EnumSource(Role.class)
  void getTeamsByUserId_isAllowed_forEveryAuthenticatedRole(Role role) throws Exception {
    mockMvc.perform(get("/api/users/7/teams").with(as(1, role))).andExpect(status().isOk());
  }

  @Test
  void getTeamsByUserId_returns401_whenUnauthenticated() throws Exception {
    mockMvc.perform(get("/api/users/7/teams")).andExpect(status().isUnauthorized());
  }

  /** Someone else's project list is readable too, on the same rule as their team list. */
  @ParameterizedTest
  @EnumSource(Role.class)
  void getProjectsByUserId_isAllowed_forEveryAuthenticatedRole(Role role) throws Exception {
    mockMvc.perform(get("/api/users/7/projects").with(as(1, role))).andExpect(status().isOk());
  }

  @Test
  void getProjectsByUserId_returns401_whenUnauthenticated() throws Exception {
    mockMvc.perform(get("/api/users/7/projects")).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"EDITOR", "ADMIN"})
  void deleteProject_isAllowed_forEveryRoleFromEditorUp(Role role) throws Exception {
    mockMvc.perform(delete("/api/projects/1").with(as(99, role))).andExpect(status().isOk());
  }

  /** Deleting is editor-only, so leading the project is not enough - unlike updating it. */
  @Test
  void deleteProject_returns403_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1").with(as(5, Role.DEVELOPER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateProject_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);

    mockMvc
        .perform(
            put("/api/projects/1")
                .with(as(5, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(UPDATE_PROJECT_BODY))
        .andExpect(status().isOk());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER"})
  void updateProject_returns403_forEveryRoleBelowEditorThatDoesNotLeadTheProject(Role role)
      throws Exception {
    mockMvc
        .perform(
            put("/api/projects/1")
                .with(as(5, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(UPDATE_PROJECT_BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void changeStatus_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);

    mockMvc
        .perform(
            patch("/api/projects/1/status")
                .with(as(5, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_STATUS_BODY))
        .andExpect(status().isOk());
  }

  /** Handing the project to someone else stays editor-only, even for the current leader. */
  @Test
  void changeProjectLeader_returns403_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);

    mockMvc
        .perform(
            patch("/api/projects/1/leader")
                .with(as(5, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_LEADER_BODY))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"EDITOR", "ADMIN"})
  void removeProjectLeader_isAllowed_forEveryRoleFromEditorUp(Role role) throws Exception {
    mockMvc.perform(delete("/api/projects/1/leader").with(as(99, role))).andExpect(status().isOk());
  }

  /** Same rule as naming a leader: the sitting leader cannot vacate the seat themselves. */
  @Test
  void removeProjectLeader_returns403_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/leader").with(as(5, Role.DEVELOPER)))
        .andExpect(status().isForbidden());
  }

  /** Reading is not restricted beyond being logged in. */
  @ParameterizedTest
  @EnumSource(Role.class)
  void getProjects_isAllowed_forEveryAuthenticatedRole(Role role) throws Exception {
    mockMvc.perform(get("/api/projects").with(as(1, role))).andExpect(status().isOk());
    mockMvc.perform(get("/api/projects/1").with(as(1, role))).andExpect(status().isOk());
  }

  /** Staffing a project is the leader's own job, so it is not editor-only. */
  @Test
  void addProjectMember_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);
    when(projectMemberService.createProjectMember(eq(1), any()))
        .thenReturn(new ProjectMemberResponse(10, 7, 1, true));

    mockMvc
        .perform(
            post("/api/projects/1/members")
                .with(as(5, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADD_MEMBER_BODY))
        .andExpect(status().isCreated());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER"})
  void addProjectMember_returns403_forEveryRoleBelowEditorThatDoesNotLeadTheProject(Role role)
      throws Exception {
    mockMvc
        .perform(
            post("/api/projects/1/members")
                .with(as(5, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADD_MEMBER_BODY))
        .andExpect(status().isForbidden());
  }

  /**
   * The trailing {@code {userId}} must not be mistaken for the project the rule authorizes against:
   * user 7 leads project 7, not project 1, so the request is still refused.
   */
  @Test
  void removeProjectMember_returns403_forTheLeaderOfADifferentProject() throws Exception {
    when(projectLookup.isLeaderOfProject(7, 7)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/members/7").with(as(7, Role.DEVELOPER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void removeProjectMember_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/members/7").with(as(5, Role.DEVELOPER)))
        .andExpect(status().isOk());
  }

  @Test
  void addProjectTeam_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);
    when(projectTeamService.createProjectTeam(eq(1), any()))
        .thenReturn(new ProjectTeamResponse(10, 3, 1, true));

    mockMvc
        .perform(
            post("/api/projects/1/teams")
                .with(as(5, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADD_PROJECT_TEAM_BODY))
        .andExpect(status().isCreated());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER"})
  void addProjectTeam_returns403_forEveryRoleBelowEditorThatDoesNotLeadTheProject(Role role)
      throws Exception {
    mockMvc
        .perform(
            post("/api/projects/1/teams")
                .with(as(5, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADD_PROJECT_TEAM_BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void removeProjectTeam_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/teams/3").with(as(5, Role.DEVELOPER)))
        .andExpect(status().isOk());
  }

  /** Rosters are readable by anyone logged in, like every other listing. */
  @ParameterizedTest
  @EnumSource(Role.class)
  void getProjectRosters_isAllowed_forEveryAuthenticatedRole(Role role) throws Exception {
    mockMvc.perform(get("/api/projects/1/members").with(as(1, role))).andExpect(status().isOk());
    mockMvc.perform(get("/api/projects/1/teams").with(as(1, role))).andExpect(status().isOk());
    mockMvc
        .perform(get("/api/projects/1/participants").with(as(1, role)))
        .andExpect(status().isOk());
  }
}
