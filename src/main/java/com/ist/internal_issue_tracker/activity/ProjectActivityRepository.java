package com.ist.internal_issue_tracker.activity;

import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Append and read back - see {@link IssueActivityRepository}. */
interface ProjectActivityRepository extends JpaRepository<ProjectActivity, Integer> {

  /**
   * Guards the replayed event. Unlike the issue and sprint versions this also keys on the values:
   * two people can be added to a project in the same operation, producing rows that agree on project,
   * action and instant and differ only in who was added.
   */
  boolean existsByProjectIdAndActionTypeAndCreatedAtAndOldValueAndNewValue(
      Integer projectId,
      ProjectActionType actionType,
      OffsetDateTime createdAt,
      String oldValue,
      String newValue);

  Page<ProjectActivity> findAllByProjectId(Integer projectId, Pageable pageable);
}
