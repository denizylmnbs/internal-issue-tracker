package com.ist.internal_issue_tracker.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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

  private static Stream<Arguments> everyOrderedRolePair() {
    return Arrays.stream(Role.values())
        .flatMap(actor -> Arrays.stream(Role.values()).map(target -> Arguments.of(actor, target)));
  }

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
   * The single point where {@link Role#outranks} and the {@link RoleHierarchy} bean are forced to
   * agree. {@code outranks} reads the enum's declaration order; the bean is declared separately in
   * {@link SecurityConfig}. Nothing links the two at runtime, so reordering the constants - or
   * inserting a role without extending the chain - would leave domain code and endpoint rules with
   * two different ideas of who outranks whom. Asserting over every ordered pair turns that into a
   * red test rather than a silent divergence.
   *
   * <p>Equality is asserted deliberately: {@code outranks} is strict, while a role is always
   * reachable from itself, so the self case has to be excluded on the hierarchy side.
   */
  @ParameterizedTest
  @MethodSource("everyOrderedRolePair")
  void outranks_agreesWithRoleHierarchy(Role actor, Role target) {
    boolean hierarchySaysStrictlyAbove =
        actor != target && reachableFrom(actor).contains(target.authority());

    assertThat(actor.outranks(target)).isEqualTo(hierarchySaysStrictlyAbove);
  }

  /**
   * Decides a {@code hasRole(...)} rule exactly the way {@code authorizeHttpRequests} does: {@code
   * AuthorizeHttpRequestsConfigurer} builds a {@link DefaultAuthorizationManagerFactory} and hands
   * it the {@code RoleHierarchy} bean. The caller carries only its own authority, as the JWT filter
   * grants it.
   */
  private boolean grants(String requiredRole, Role callerRole) {
    DefaultAuthorizationManagerFactory<Object> factory = new DefaultAuthorizationManagerFactory<>();
    factory.setRoleHierarchy(roleHierarchy);

    Authentication caller =
        new UsernamePasswordAuthenticationToken(
            "caller", null, List.of(new SimpleGrantedAuthority(callerRole.authority())));
    AuthorizationResult result =
        factory.hasRole(requiredRole).authorize(() -> caller, new Object());

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
