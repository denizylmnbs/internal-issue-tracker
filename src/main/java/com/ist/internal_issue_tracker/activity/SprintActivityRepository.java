package com.ist.internal_issue_tracker.activity;

import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Append and read back - see {@link IssueActivityRepository}. */
interface SprintActivityRepository extends JpaRepository<SprintActivity, Integer> {

  /**
   * Guards the replayed event, and is a check rather than an index for the reason given on {@link
   * IssueActivityRepository#existsByIssueIdAndActionTypeAndCreatedAt}.
   */
  boolean existsBySprintIdAndActionTypeAndCreatedAt(
      Integer sprintId, SprintActionType actionType, OffsetDateTime createdAt);

  Page<SprintActivity> findAllBySprintId(Integer sprintId, Pageable pageable);
}
