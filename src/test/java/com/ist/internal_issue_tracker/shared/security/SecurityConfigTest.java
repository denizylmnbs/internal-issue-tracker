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

import com.ist.internal_issue_tracker.comment.CommentController;
import com.ist.internal_issue_tracker.comment.CommentService;
import com.ist.internal_issue_tracker.comment.dto.CommentResponse;
import com.ist.internal_issue_tracker.epic.EpicController;
import com.ist.internal_issue_tracker.epic.EpicService;
import com.ist.internal_issue_tracker.epic.EpicStatus;
import com.ist.internal_issue_tracker.epic.dto.EpicResponse;
import com.ist.internal_issue_tracker.issue.IssueController;
import com.ist.internal_issue_tracker.issue.IssuePriority;
import com.ist.internal_issue_tracker.issue.IssueService;
import com.ist.internal_issue_tracker.issue.IssueStatus;
import com.ist.internal_issue_tracker.issue.IssueType;
import com.ist.internal_issue_tracker.issue.dto.IssueResponse;
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
import com.ist.internal_issue_tracker.sprint.SprintController;
import com.ist.internal_issue_tracker.sprint.SprintService;
import com.ist.internal_issue_tracker.sprint.SprintStatus;
import com.ist.internal_issue_tracker.sprint.dto.SprintResponse;
import com.ist.internal_issue_tracker.team.TeamController;
import com.ist.internal_issue_tracker.team.TeamMemberController;
import com.ist.internal_issue_tracker.team.TeamMemberService;
import com.ist.internal_issue_tracker.team.TeamService;
import com.ist.internal_issue_tracker.team.UserTeamsController;
import com.ist.internal_issue_tracker.team.dto.TeamMemberResponse;
import com.ist.internal_issue_tracker.user.UserController;
import com.ist.internal_issue_tracker.user.UserService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
      UserProjectsController.class,
      SprintController.class,
      EpicController.class,
      IssueController.class,
      CommentController.class
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
  private static final String SPRINT_BODY =
      "{\"name\":\"Sprint 1\",\"description\":\"x\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-01-15\"}";
  private static final String CHANGE_SPRINT_STATUS_BODY = "{\"status\":\"IN_PROGRESS\"}";
  private static final String EPIC_BODY = "{\"name\":\"Checkout rewrite\",\"description\":\"x\"}";
  private static final String CHANGE_EPIC_STATUS_BODY = "{\"status\":\"ON_HOLD\"}";
  private static final String ISSUE_BODY =
      "{\"name\":\"Login fails\",\"description\":\"x\",\"type\":\"BUG\",\"priority\":\"HIGH\"}";
  private static final String CHANGE_ISSUE_STATUS_BODY = "{\"status\":\"IN_PROGRESS\"}";
  private static final String CHANGE_ASSIGNEE_BODY = "{\"assigneeUserId\":7,\"assigneeTeamId\":3}";
  private static final String COMMENT_BODY = "{\"content\":\"Reproduced on staging.\"}";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;
  @MockitoBean private TeamService teamService;
  @MockitoBean private TeamMemberService teamMemberService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private AuthenticatedUserLookup authenticatedUserLookup;

  @MockitoBean private ProjectService projectService;
  @MockitoBean private ProjectMemberService projectMemberService;
  @MockitoBean private ProjectTeamService projectTeamService;
  @MockitoBean private SprintService sprintService;
  @MockitoBean private EpicService epicService;
  @MockitoBean private IssueService issueService;
  @MockitoBean private CommentService commentService;

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
        .thenReturn(new TeamMemberResponse(10, 7, 1, true, OffsetDateTime.now()));

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
        .thenReturn(new TeamMemberResponse(10, 7, 1, true, OffsetDateTime.now()));

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
        .thenReturn(new ProjectMemberResponse(10, 7, 1, true, OffsetDateTime.now()));

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
        .thenReturn(new ProjectTeamResponse(10, 3, 1, true, OffsetDateTime.now()));

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

  private static SprintResponse sprintResponse() {
    return new SprintResponse(
        10,
        1,
        "Sprint 1",
        "x",
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 15),
        SprintStatus.TODO,
        OffsetDateTime.now(),
        OffsetDateTime.now());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"EDITOR", "ADMIN"})
  void createSprint_isAllowed_forEveryRoleFromEditorUp(Role role) throws Exception {
    when(sprintService.createSprint(eq(1), any())).thenReturn(sprintResponse());

    mockMvc
        .perform(
            post("/api/projects/1/sprints")
                .with(as(99, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(SPRINT_BODY))
        .andExpect(status().isCreated());
  }

  /** Planning a project's sprints is the leader's own job, so it is not editor-only. */
  @Test
  void createSprint_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);
    when(sprintService.createSprint(eq(1), any())).thenReturn(sprintResponse());

    mockMvc
        .perform(
            post("/api/projects/1/sprints")
                .with(as(5, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(SPRINT_BODY))
        .andExpect(status().isCreated());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER"})
  void createSprint_returns403_forEveryRoleBelowEditorThatDoesNotLeadTheProject(Role role)
      throws Exception {
    mockMvc
        .perform(
            post("/api/projects/1/sprints")
                .with(as(5, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(SPRINT_BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateSprint_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);
    when(sprintService.updateSprint(eq(1), eq(10), any())).thenReturn(sprintResponse());

    mockMvc
        .perform(
            put("/api/projects/1/sprints/10")
                .with(as(5, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(SPRINT_BODY))
        .andExpect(status().isOk());
  }

  @Test
  void changeSprintStatus_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);
    when(sprintService.changeStatus(eq(1), eq(10), any())).thenReturn(sprintResponse());

    mockMvc
        .perform(
            patch("/api/projects/1/sprints/10/status")
                .with(as(5, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_SPRINT_STATUS_BODY))
        .andExpect(status().isOk());
  }

  @Test
  void deleteSprint_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/sprints/10").with(as(5, Role.DEVELOPER)))
        .andExpect(status().isOk());
  }

  /**
   * The trailing {@code {sprintId}} must not be mistaken for the project the rule authorizes
   * against: user 10 leads project 10, not project 1, so the request is still refused. The service
   * layer then has to make the same distinction for its own reasons - see {@code
   * SprintRepository#findByIdAndProjectIdAndDeletedAtIsNull}.
   */
  @Test
  void deleteSprint_returns403_forTheLeaderOfADifferentProject() throws Exception {
    when(projectLookup.isLeaderOfProject(10, 10)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/sprints/10").with(as(10, Role.DEVELOPER)))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER"})
  void changeSprintStatus_returns403_forEveryRoleBelowEditorThatDoesNotLeadTheProject(Role role)
      throws Exception {
    mockMvc
        .perform(
            patch("/api/projects/1/sprints/10/status")
                .with(as(5, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_SPRINT_STATUS_BODY))
        .andExpect(status().isForbidden());
  }

  /** Sprints are readable by anyone logged in, like every other listing. */
  @ParameterizedTest
  @EnumSource(Role.class)
  void getSprints_isAllowed_forEveryAuthenticatedRole(Role role) throws Exception {
    mockMvc.perform(get("/api/projects/1/sprints").with(as(1, role))).andExpect(status().isOk());
    mockMvc.perform(get("/api/projects/1/sprints/10").with(as(1, role))).andExpect(status().isOk());
  }

  private static EpicResponse epicResponse() {
    return new EpicResponse(
        20,
        1,
        "Checkout rewrite",
        "x",
        EpicStatus.TODO,
        99,
        OffsetDateTime.now(),
        OffsetDateTime.now());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"EDITOR", "ADMIN"})
  void createEpic_isAllowed_forEveryRoleFromEditorUp(Role role) throws Exception {
    when(epicService.createEpic(eq(1), eq(99), any())).thenReturn(epicResponse());

    mockMvc
        .perform(
            post("/api/projects/1/epics")
                .with(as(99, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(EPIC_BODY))
        .andExpect(status().isCreated());
  }

  /**
   * Doubles as the check that the reporter is the caller: the stub only answers for reporter 5, so
   * a 201 here means the controller passed the principal's id and not something from the body.
   */
  @Test
  void createEpic_isAllowed_forTheLeaderOfThatProject_andReportsThemAsTheReporter()
      throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);
    when(epicService.createEpic(eq(1), eq(5), any())).thenReturn(epicResponse());

    mockMvc
        .perform(
            post("/api/projects/1/epics")
                .with(as(5, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(EPIC_BODY))
        .andExpect(status().isCreated());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER"})
  void createEpic_returns403_forEveryRoleBelowEditorThatDoesNotLeadTheProject(Role role)
      throws Exception {
    mockMvc
        .perform(
            post("/api/projects/1/epics")
                .with(as(5, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(EPIC_BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateEpic_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);
    when(epicService.updateEpic(eq(1), eq(20), any())).thenReturn(epicResponse());

    mockMvc
        .perform(
            put("/api/projects/1/epics/20")
                .with(as(5, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(EPIC_BODY))
        .andExpect(status().isOk());
  }

  @Test
  void changeEpicStatus_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);
    when(epicService.changeStatus(eq(1), eq(20), any())).thenReturn(epicResponse());

    mockMvc
        .perform(
            patch("/api/projects/1/epics/20/status")
                .with(as(5, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_EPIC_STATUS_BODY))
        .andExpect(status().isOk());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER"})
  void changeEpicStatus_returns403_forEveryRoleBelowEditorThatDoesNotLeadTheProject(Role role)
      throws Exception {
    mockMvc
        .perform(
            patch("/api/projects/1/epics/20/status")
                .with(as(5, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_EPIC_STATUS_BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteEpic_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/epics/20").with(as(5, Role.DEVELOPER)))
        .andExpect(status().isOk());
  }

  /**
   * The trailing {@code {epicId}} must not be mistaken for the project the rule authorizes against:
   * user 20 leads project 20, not project 1, so the request is still refused - see {@code
   * EpicRepository#findByIdAndProjectIdAndDeletedAtIsNull} for the service-layer half of the same
   * distinction.
   */
  @Test
  void deleteEpic_returns403_forTheLeaderOfADifferentProject() throws Exception {
    when(projectLookup.isLeaderOfProject(20, 20)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/epics/20").with(as(20, Role.DEVELOPER)))
        .andExpect(status().isForbidden());
  }

  /** Epics are readable by anyone logged in, like every other listing. */
  @ParameterizedTest
  @EnumSource(Role.class)
  void getEpics_isAllowed_forEveryAuthenticatedRole(Role role) throws Exception {
    mockMvc.perform(get("/api/projects/1/epics").with(as(1, role))).andExpect(status().isOk());
    mockMvc.perform(get("/api/projects/1/epics/20").with(as(1, role))).andExpect(status().isOk());
  }

  private static IssueResponse issueResponse() {
    return new IssueResponse(
        30,
        1,
        null,
        null,
        IssueType.BUG,
        "Login fails",
        "x",
        IssueStatus.BACKLOG,
        IssuePriority.HIGH,
        null,
        99,
        null,
        null,
        OffsetDateTime.now(),
        OffsetDateTime.now());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"EDITOR", "ADMIN"})
  void createIssue_isAllowed_forEveryRoleFromEditorUp(Role role) throws Exception {
    when(issueService.createIssue(eq(1), eq(99), any())).thenReturn(issueResponse());

    mockMvc
        .perform(
            post("/api/projects/1/issues")
                .with(as(99, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ISSUE_BODY))
        .andExpect(status().isCreated());
  }

  @Test
  void createIssue_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);
    when(issueService.createIssue(eq(1), eq(5), any())).thenReturn(issueResponse());

    mockMvc
        .perform(
            post("/api/projects/1/issues")
                .with(as(5, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ISSUE_BODY))
        .andExpect(status().isCreated());
  }

  /**
   * The rule these routes exist to prove: a developer who neither is an editor nor leads the project
   * may still file work on it, so long as they actually work on it.
   */
  @Test
  void createIssue_isAllowed_forAParticipantOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 6)).thenReturn(false);
    when(projectLookup.isParticipantOfProject(1, 6)).thenReturn(true);
    when(issueService.createIssue(eq(1), eq(6), any())).thenReturn(issueResponse());

    mockMvc
        .perform(
            post("/api/projects/1/issues")
                .with(as(6, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ISSUE_BODY))
        .andExpect(status().isCreated());
  }

  /** Both ports answer no, so having the role alone is not enough. */
  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER"})
  void createIssue_returns403_forEveryRoleBelowEditorOffTheProject(Role role) throws Exception {
    mockMvc
        .perform(
            post("/api/projects/1/issues")
                .with(as(6, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ISSUE_BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateIssue_isAllowed_forAParticipantOfThatProject() throws Exception {
    when(projectLookup.isParticipantOfProject(1, 6)).thenReturn(true);
    when(issueService.updateIssue(eq(1), eq(30), any())).thenReturn(issueResponse());

    mockMvc
        .perform(
            put("/api/projects/1/issues/30")
                .with(as(6, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Login fails\",\"description\":\"x\",\"type\":\"BUG\","
                        + "\"priority\":\"HIGH\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void changeIssueStatus_isAllowed_forAParticipantOfThatProject() throws Exception {
    when(projectLookup.isParticipantOfProject(1, 6)).thenReturn(true);
    when(issueService.changeStatus(eq(1), eq(30), any())).thenReturn(issueResponse());

    mockMvc
        .perform(
            patch("/api/projects/1/issues/30/status")
                .with(as(6, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_ISSUE_STATUS_BODY))
        .andExpect(status().isOk());
  }

  @Test
  void changeIssueAssignee_isAllowed_forAParticipantOfThatProject() throws Exception {
    when(projectLookup.isParticipantOfProject(1, 6)).thenReturn(true);
    when(issueService.changeAssignee(eq(1), eq(30), any())).thenReturn(issueResponse());

    mockMvc
        .perform(
            patch("/api/projects/1/issues/30/assignee")
                .with(as(6, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGE_ASSIGNEE_BODY))
        .andExpect(status().isOk());
  }

  @Test
  void removeIssueAssignee_isAllowed_forAParticipantOfThatProject() throws Exception {
    when(projectLookup.isParticipantOfProject(1, 6)).thenReturn(true);
    when(issueService.removeAssignee(1, 30)).thenReturn(issueResponse());

    mockMvc
        .perform(delete("/api/projects/1/issues/30/assignee").with(as(6, Role.DEVELOPER)))
        .andExpect(status().isOk());
  }

  /**
   * Deleting is the one issue route participation does not open. The stub says user 6 works on the
   * project and it still makes no difference, which is what separates this matcher from the five
   * above it.
   */
  @Test
  void deleteIssue_returns403_forAParticipantWhoDoesNotLeadTheProject() throws Exception {
    when(projectLookup.isParticipantOfProject(1, 6)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/issues/30").with(as(6, Role.DEVELOPER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteIssue_isAllowed_forTheLeaderOfThatProject() throws Exception {
    when(projectLookup.isLeaderOfProject(1, 5)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/issues/30").with(as(5, Role.DEVELOPER)))
        .andExpect(status().isOk());
  }

  /**
   * The trailing {@code {issueId}} must not be mistaken for the project the rule authorizes against:
   * user 30 leads project 30, not project 1.
   */
  @Test
  void deleteIssue_returns403_forTheLeaderOfADifferentProject() throws Exception {
    when(projectLookup.isLeaderOfProject(30, 30)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/issues/30").with(as(30, Role.DEVELOPER)))
        .andExpect(status().isForbidden());
  }

  /** Reading is not restricted beyond being logged in, participation or not. */
  @ParameterizedTest
  @EnumSource(Role.class)
  void getIssues_isAllowed_forEveryAuthenticatedRole(Role role) throws Exception {
    mockMvc.perform(get("/api/projects/1/issues").with(as(1, role))).andExpect(status().isOk());
    mockMvc.perform(get("/api/projects/1/issues/30").with(as(1, role))).andExpect(status().isOk());
  }

  private static CommentResponse commentResponse() {
    return new CommentResponse(
        40, 30, 6, "Reproduced on staging.", OffsetDateTime.now(), OffsetDateTime.now());
  }

  /**
   * These tests cover the coarse gate only. Whether a caller who gets through may touch a
   * <em>particular</em> comment is decided by {@code CommentService} from the author column, which a
   * request matcher cannot see and this slice therefore cannot exercise.
   */
  @Test
  void createComment_isAllowed_forAParticipantOfThatProject() throws Exception {
    when(projectLookup.isParticipantOfProject(1, 6)).thenReturn(true);
    when(commentService.createComment(eq(1), eq(30), eq(6), any())).thenReturn(commentResponse());

    mockMvc
        .perform(
            post("/api/projects/1/issues/30/comments")
                .with(as(6, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(COMMENT_BODY))
        .andExpect(status().isCreated());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"EDITOR", "ADMIN"})
  void createComment_isAllowed_forEveryRoleFromEditorUp(Role role) throws Exception {
    when(commentService.createComment(eq(1), eq(30), eq(99), any())).thenReturn(commentResponse());

    mockMvc
        .perform(
            post("/api/projects/1/issues/30/comments")
                .with(as(99, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(COMMENT_BODY))
        .andExpect(status().isCreated());
  }

  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"USER", "DEVELOPER"})
  void createComment_returns403_forEveryRoleBelowEditorOffTheProject(Role role) throws Exception {
    mockMvc
        .perform(
            post("/api/projects/1/issues/30/comments")
                .with(as(6, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(COMMENT_BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateComment_passesTheGate_forAParticipantOfThatProject() throws Exception {
    when(projectLookup.isParticipantOfProject(1, 6)).thenReturn(true);
    when(commentService.updateComment(eq(1), eq(30), eq(40), any(), any()))
        .thenReturn(commentResponse());

    mockMvc
        .perform(
            put("/api/projects/1/issues/30/comments/40")
                .with(as(6, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(COMMENT_BODY))
        .andExpect(status().isOk());
  }

  @Test
  void updateComment_returns403_forSomeoneOffTheProject() throws Exception {
    mockMvc
        .perform(
            put("/api/projects/1/issues/30/comments/40")
                .with(as(6, Role.DEVELOPER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(COMMENT_BODY))
        .andExpect(status().isForbidden());
  }

  /** Deleting is gated the same way as editing; the two diverge inside the service, not here. */
  @Test
  void deleteComment_passesTheGate_forAParticipantOfThatProject() throws Exception {
    when(projectLookup.isParticipantOfProject(1, 6)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/issues/30/comments/40").with(as(6, Role.DEVELOPER)))
        .andExpect(status().isOk());
  }

  @Test
  void deleteComment_returns403_forSomeoneOffTheProject() throws Exception {
    mockMvc
        .perform(delete("/api/projects/1/issues/30/comments/40").with(as(6, Role.DEVELOPER)))
        .andExpect(status().isForbidden());
  }

  /**
   * The rule reads {@code {id}} and nothing else, however many path variables follow it: user 40
   * leads project 40, not project 1.
   */
  @Test
  void deleteComment_returns403_forTheLeaderOfADifferentProject() throws Exception {
    when(projectLookup.isLeaderOfProject(40, 40)).thenReturn(true);

    mockMvc
        .perform(delete("/api/projects/1/issues/30/comments/40").with(as(40, Role.DEVELOPER)))
        .andExpect(status().isForbidden());
  }

  /** Reading a thread is not restricted beyond being logged in. */
  @ParameterizedTest
  @EnumSource(Role.class)
  void getComments_isAllowed_forEveryAuthenticatedRole(Role role) throws Exception {
    mockMvc
        .perform(get("/api/projects/1/issues/30/comments").with(as(1, role)))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/projects/1/issues/30/comments/40").with(as(1, role)))
        .andExpect(status().isOk());
  }
}
