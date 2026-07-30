package com.ist.internal_issue_tracker.shared.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
      UserTeamsController.class
    })
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class SecurityConfigTest {

  private static final String RESET_PASSWORD_BODY = "{\"newPassword\":\"password123\"}";
  private static final String CHANGE_PASSWORD_BODY =
      "{\"currentPassword\":\"password123\",\"newPassword\":\"password456\"}";
  private static final String CHANGE_ROLE_BODY = "{\"newRole\":\"EDITOR\"}";
  private static final String ADD_MEMBER_BODY = "{\"userId\":7}";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;
  @MockitoBean private TeamService teamService;
  @MockitoBean private TeamMemberService teamMemberService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private AuthenticatedUserLookup authenticatedUserLookup;

  /** {@code securityFilterChain} takes the port directly, so the slice has to supply it. */
  @MockitoBean private TeamLookup teamLookup;

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
}
