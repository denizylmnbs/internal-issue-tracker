package com.ist.internal_issue_tracker.activity;

import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Append and read back, nothing else - there is no update or delete path, because a history that
 * can be edited is not one.
 */
interface IssueActivityRepository extends JpaRepository<IssueActivity, Integer> {

  /**
   * Guards against the duplicate a replayed event would otherwise write.
   *
   * <p>Kafka delivery is at-least-once: a consumer that writes its row and dies before its offset
   * is committed will be handed the same record again, and an offset rewound by hand will hand back
   * the whole topic. The three columns together identify a row uniquely in practice, since one
   * operation cannot touch the same field twice at the same instant.
   *
   * <p>This is a check rather than a unique index on purpose. A constraint violation raised inside
   * a consumer fails the record rather than the request, so the same event would be retried, fail
   * again, and after its retries are spent be set aside in a dead letter topic - an operational
   * problem raised for something that was never wrong. Losing the race here costs a duplicate row;
   * losing it against an index costs a false alarm, or a stalled partition.
   */
  boolean existsByIssueIdAndActionTypeAndCreatedAt(
      Integer issueId, IssueActionType actionType, OffsetDateTime createdAt);

  Page<IssueActivity> findAllByIssueId(Integer issueId, Pageable pageable);
}
