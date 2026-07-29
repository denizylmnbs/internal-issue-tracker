package com.ist.internal_issue_tracker.shared.security;

/**
 * Global role carried by every user, declared in increasing order of privilege.
 *
 * <p>Endpoint rules never consult this order. Which role implies which is declared once as the
 * {@code RoleHierarchy} bean in {@link SecurityConfig}, so that a rule only ever has to state the
 * <em>minimum</em> role it requires and Spring expands the rest at decision time.
 *
 * <p>{@link #outranks} and {@link #atLeast} exist for domain code, which has to compare two roles
 * it holds in hand - a question no {@code RoleHierarchy} lookup phrases naturally. They read the
 * declaration order above, which makes it a second statement of the same hierarchy; {@code
 * RoleHierarchyTest} asserts the two agree over every ordered pair so they cannot drift.
 */
public enum Role {
  USER,
  DEVELOPER,
  EDITOR,
  ADMIN;

  /**
   * Spring Security matches {@code hasRole("ADMIN")} against an authority named {@code ROLE_ADMIN},
   * so the prefix is applied in exactly one place rather than spelled out at every call site - a
   * mismatched prefix fails silently rather than loudly.
   */
  public String authority() {
    return "ROLE_" + name();
  }

  /** {@code true} when this role ranks strictly above {@code other}. */
  public boolean outranks(Role other) {
    return this.ordinal() > other.ordinal();
  }

  /** {@code true} when this role is {@code other} or ranks above it. */
  public boolean atLeast(Role other) {
    return this == other || outranks(other);
  }
}
