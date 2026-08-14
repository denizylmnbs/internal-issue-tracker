package com.ist.internal_issue_tracker.shared.port;

/**
 * The other direction from {@link FieldDefinitionLookup}: before {@code fielddef} retires a code,
 * it asks whoever writes that code onto their own rows how many currently carry it and, if the
 * caller supplies a replacement, moves them over. One implementation per module that stores a code
 * of one of these kinds ({@code issue}, {@code sprint}, {@code epic}, {@code project}, {@code
 * team}), each covering the kind(s) it owns, so {@code fielddef} never needs to know their entity
 * types.
 */
public interface FieldCodeUsageResolver {

  /** Whether this resolver is the one that owns {@code kind}'s storage. */
  boolean supports(FieldKind kind);

  /**
   * How many live rows of this kind currently carry {@code code}. {@code projectId} is ignored for
   * kinds where {@link FieldKind#isGlobal()} is {@code true}, matching every other port here.
   */
  long countUsages(FieldKind kind, Integer projectId, String code);

  /** Moves every live row carrying {@code fromCode} onto {@code toCode}. */
  void reassign(FieldKind kind, Integer projectId, String fromCode, String toCode);
}
