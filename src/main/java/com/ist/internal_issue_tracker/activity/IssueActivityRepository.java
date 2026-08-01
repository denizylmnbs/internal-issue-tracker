package com.ist.internal_issue_tracker.activity;

import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Append and read back, nothing else - there is no update or delete path, because a history that can
 * be edited is not one.
 */
interface IssueActivityRepository extends JpaRepository<IssueActivity, Integer> {

  /**
   * Guards against the duplicate a replayed event would otherwise write.
   *
   * <p>Delivery through the publication registry is at-least-once: a listener that succeeds but
   * crashes before its publication is marked complete will be handed the same event again on the
   * next start. The three columns together identify a row uniquely in practice, since one operation
   * cannot touch the same field twice at the same instant.
   *
   * <p>This is a check rather than a unique index on purpose. A constraint violation raised inside
   * an asynchronous listener fails the publication instead of the request, so the same event would
   * be retried and fail again on every restart, forever. Losing the race here costs a duplicate row;
   * losing it against an index costs a stuck queue.
   */
  boolean existsByIssueIdAndActionTypeAndCreatedAt(
      Integer issueId, IssueActionType actionType, OffsetDateTime createdAt);

  Page<IssueActivity> findAllByIssueId(Integer issueId, Pageable pageable);
}
