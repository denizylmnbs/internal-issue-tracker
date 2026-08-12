package com.ist.internal_issue_tracker.comment;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Derived and JPQL, no native SQL. A comment points at an issue and a user but only by id - both
 * are validated through ports, never joined - so every query stays inside one table and sorting
 * keeps working on the list endpoint.
 */
interface CommentRepository extends JpaRepository<Comment, Integer> {

  /**
   * Both keys, always. The authorization rule only ever sees the project, so a comment id belonging
   * to a different issue has to be caught here or not at all.
   */
  Optional<Comment> findByIdAndIssueIdAndDeletedAtIsNull(Integer id, Integer issueId);

  /**
   * One issue's thread, deleted comments excluded unconditionally. The {@code userId} filter is
   * what the schema's index on {@code comments(user_id)} is for - "everything so-and-so said on
   * this issue" without a second endpoint to ask it.
   */
  @Query(
      """
      SELECT c FROM Comment c
      WHERE c.issueId = :issueId
      AND c.deletedAt IS NULL
      AND (:userId IS NULL OR c.userId = :userId)
      """)
  Page<Comment> findAllByFilters(
      @Param("issueId") Integer issueId, @Param("userId") Integer userId, Pageable pageable);
}
