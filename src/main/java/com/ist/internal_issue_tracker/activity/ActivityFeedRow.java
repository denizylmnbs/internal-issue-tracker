package com.ist.internal_issue_tracker.activity;

import java.time.Instant;

/**
 * Row shape of the project-wide activity feed's union query ({@link
 * ProjectActivityRepository#findFeedByProjectId}). An interface projection because the query is
 * native — the same reason {@code project.ProjectParticipant} and {@code project.UserProject} are.
 *
 * <p>{@code scope} and {@code subjectId} come back as literal columns the union assigns per branch
 * (see the query), not from any one table — turning {@code scope} into {@link ActivityScope} is
 * {@code ActivityMapper}'s job, same as {@code UserProject#getProjectStatus} being converted there
 * rather than here.
 *
 * <p>{@code createdAt} is {@link Instant}, not {@code OffsetDateTime}: this is a native query, so
 * Hibernate hands the projection proxy back whatever type it reads {@code timestamptz} as, and the
 * proxy only converts between an interface and its target - not between two concrete temporal
 * types. {@code ActivityMapper} converts to {@code OffsetDateTime} when building {@code
 * ActivityResponse}.
 */
public interface ActivityFeedRow {

  Integer getId();

  Integer getUserId();

  String getActionType();

  String getOldValue();

  String getNewValue();

  Instant getCreatedAt();

  String getScope();

  Integer getSubjectId();
}
