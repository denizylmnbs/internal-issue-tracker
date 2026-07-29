package com.ist.internal_issue_tracker.shared.security;

/**
 * Global role carried by every user, declared in increasing order of privilege.
 *
 * <p>The hierarchy itself - which role implies which - is deliberately <em>not</em> encoded here. It
 * is declared once as the {@code RoleHierarchy} bean in {@link SecurityConfig}, so that
 * authorization rules only ever have to state the <em>minimum</em> role an endpoint requires. Adding
 * a second notion of "implies" to this enum would let the two drift apart.
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

  public boolean outranks(Role other) {
    return this.ordinal() > other.ordinal();
  }
}
