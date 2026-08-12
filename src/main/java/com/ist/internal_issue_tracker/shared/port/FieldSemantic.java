package com.ist.internal_issue_tracker.shared.port;

/**
 * The meanings a field definition row can carry, independent of what its code happens to be
 * spelled as.
 *
 * <p>Metrics that used to match on literal status names (a hardcoded {@code 'DONE'} or {@code
 * 'BUG'} string) now ask {@link FieldDefinitionLookup#codesWithSemantic} for the set of codes
 * that currently carry a given semantic, and bind that set into the query instead. A project that
 * renames {@code DONE} to {@code SHIPPED}, or adds a second done-equivalent status, changes which
 * codes come back without changing a line of SQL.
 */
public enum FieldSemantic {
  /** Work is delivered. Counted as completed throughput; excluded from work-in-progress. */
  DONE,

  /** Work left the flow without being delivered. Excluded from every completion metric. */
  CANCELLED,

  /** Work is actively being progressed, as opposed to waiting. What flow efficiency divides by. */
  ACTIVE_WORK,

  /** For {@link FieldKind#ISSUE_TYPE}: this type counts as a defect for defect-ratio and MTTR. */
  DEFECT
}
