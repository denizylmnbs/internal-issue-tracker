package com.ist.internal_issue_tracker.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.DefaultAuthorizationManagerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Pins the role hierarchy itself, independently of any endpoint. {@link SecurityConfig} is
 * instantiated directly - the bean has no collaborators, so no Spring context is needed and these
 * assertions stay readable as the plain statements of intent they are.
 */
class RoleHierarchyTest {

  private final RoleHierarchy roleHierarchy = new SecurityConfig().roleHierarchy();

  private List<String> reachableFrom(Role role) {
    return roleHierarchy
        .getReachableGrantedAuthorities(List.of(new SimpleGrantedAuthority(role.authority())))
        .stream()
        .map(GrantedAuthority::getAuthority)
        .toList();
  }

  @Test
  void admin_impliesEveryOtherRole() {
    assertThat(reachableFrom(Role.ADMIN))
        .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_EDITOR", "ROLE_DEVELOPER", "ROLE_USER");
  }

  /**
   * The hierarchy is declared as a chain of single steps, so this also proves the closure is
   * transitive: {@code EDITOR -> DEVELOPER} and {@code DEVELOPER -> USER} were never spelled out
   * together.
   */
  @Test
  void editor_impliesDeveloperAndUser_butNotAdmin() {
    assertThat(reachableFrom(Role.EDITOR))
        .containsExactlyInAnyOrder("ROLE_EDITOR", "ROLE_DEVELOPER", "ROLE_USER");
  }

  @Test
  void developer_impliesUser_butNotEditor() {
    assertThat(reachableFrom(Role.DEVELOPER))
        .containsExactlyInAnyOrder("ROLE_DEVELOPER", "ROLE_USER");
  }

  @Test
  void user_impliesNothingFurther() {
    assertThat(reachableFrom(Role.USER)).containsExactly("ROLE_USER");
  }

  /**
   * Guards the {@code ROLE_} prefix contract: {@code withDefaultRolePrefix()} and {@code
   * hasRole(...)} both assume it, and a mismatch here would make every rule silently deny.
   */
  @Test
  void authorityName_carriesTheRolePrefix() {
    assertThat(Role.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
  }

  /**
   * Decides a {@code hasRole(...)} rule exactly the way {@code authorizeHttpRequests} does:
   * {@code AuthorizeHttpRequestsConfigurer} builds a {@link DefaultAuthorizationManagerFactory} and
   * hands it the {@code RoleHierarchy} bean. The caller carries only its own authority, as the JWT
   * filter grants it.
   */
  private boolean grants(String requiredRole, Role callerRole) {
    DefaultAuthorizationManagerFactory<Object> factory = new DefaultAuthorizationManagerFactory<>();
    factory.setRoleHierarchy(roleHierarchy);

    Authentication caller =
        new UsernamePasswordAuthenticationToken(
            "caller", null, List.of(new SimpleGrantedAuthority(callerRole.authority())));
    AuthorizationResult result = factory.hasRole(requiredRole).authorize(() -> caller, new Object());

    return result != null && result.isGranted();
  }

  /**
   * The point of the whole exercise: a rule states only its <em>minimum</em> role, and everyone
   * above it is admitted without the rule mentioning them.
   */
  @ParameterizedTest
  @EnumSource(
      value = Role.class,
      names = {"DEVELOPER", "EDITOR", "ADMIN"})
  void aDeveloperRule_admitsEveryRoleFromDeveloperUp(Role callerRole) {
    assertThat(grants("DEVELOPER", callerRole)).isTrue();
  }

  @Test
  void aDeveloperRule_deniesUser() {
    assertThat(grants("DEVELOPER", Role.USER)).isFalse();
  }

  @Test
  void anAdminRule_deniesEditor() {
    assertThat(grants("ADMIN", Role.EDITOR)).isFalse();
  }
}
