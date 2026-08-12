package com.ist.internal_issue_tracker.shared.port;

import java.util.Set;

/**
 * Whether a status/type/priority/etc. code is one a project has actually defined, without the
 * asking module ever naming a type from {@code fielddef}.
 *
 * <p>Every module that writes a classification onto its own entity - {@code issue}, {@code
 * sprint}, {@code epic}, {@code project}, {@code team} - calls {@link #isValidCode} before storing
 * a caller-supplied code, and {@code activity.metrics} calls {@link #codesWithSemantic} to turn a
 * hardcoded literal like {@code 'DONE'} into a bound parameter. {@code projectId} is ignored for
 * kinds where {@link FieldKind#isGlobal()} is {@code true}.
 */
public interface FieldDefinitionLookup {

  /** {@code true} if an active field definition with this code exists for the kind/project. */
  boolean isValidCode(Integer projectId, FieldKind kind, String code);

  /**
   * The code new rows of this kind get when nothing is specified - {@code BACKLOG} for a fresh
   * issue, {@code PLANNING} for a fresh project. Never null for a kind that has been seeded;
   * seeding is what guarantees exactly one default exists.
   */
  String defaultCode(Integer projectId, FieldKind kind);

  /**
   * Every active code of this kind, for this project, that carries the given semantic. Empty when
   * nothing does - a project need not mark any status {@code CANCELLED}-equivalent, for instance.
   * Callers binding this into a native {@code IN (...)} clause must handle the empty case
   * themselves; an empty collection is not valid SQL there.
   */
  Set<String> codesWithSemantic(Integer projectId, FieldKind kind, FieldSemantic semantic);
}
