package com.ist.internal_issue_tracker.activity.metrics;

import java.time.Instant;

/**
 * Bugs against everything else, in one bucket, from both ends: how many arrived and how much
 * shipped.
 *
 * <p>Two different metrics share this shape because they share a denominator problem.
 *
 * <p><em>Bug share</em> ({@code createdBugShare}) is bugs as a fraction of everything filed. It is
 * the easy one and the weaker one: it moves when feature work slows down, without anything about
 * quality having changed.
 *
 * <p><em>Defect density</em> ({@code defectsPerCompletedIssue}, {@code defectsPerCompletedPoint})
 * is bugs filed against work delivered, which is the ratio that actually tracks quality - it asks
 * how many defects each unit of shipped work brought with it. Both variants are returned because
 * neither denominator is right on its own: per-issue is stable on teams that estimate patchily, and
 * per-point is the fairer comparison where estimates are consistent. A team that does not estimate
 * gets null for the second, which is the honest answer.
 *
 * <p>Bugs are counted where they were <em>filed</em>, not where they were fixed. A defect belongs
 * to the period that produced it, and attributing it to the sprint that cleaned it up would reward
 * leaving bugs unfixed.
 *
 * <p>Every ratio is null rather than zero when its denominator is empty - see {@code
 * DurationStats}.
 */
public interface DefectBucket {

  Instant getBucketStart();

  Long getCreatedCount();

  Long getCreatedBugCount();

  Double getCreatedBugShare();

  Long getCompletedCount();

  Long getCompletedBugCount();

  Long getCompletedStoryPoints();

  Double getDefectsPerCompletedIssue();

  Double getDefectsPerCompletedPoint();
}
