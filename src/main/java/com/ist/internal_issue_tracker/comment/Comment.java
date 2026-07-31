package com.ist.internal_issue_tracker.comment;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * One remark on an issue.
 *
 * <p>The author is what makes this table different from every other one so far: who wrote a comment
 * decides who may change it, and that fact lives here rather than in the request path, so the rule
 * cannot be enforced in {@code SecurityConfig} and drops into {@code CommentService} instead.
 *
 * <p>Soft delete is a {@code deletedAt} stamp, as in {@code Issue}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "comments")
public class Comment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  /**
   * Plain user id rather than a {@code @ManyToOne User} - an association would cross into the {@code
   * user} module and break {@code ModularityTests}.
   *
   * <p>{@code updatable = false} because authorship is a historical fact: no endpoint changes it,
   * and marking the column that way means no future one can do so by accident. The whole ownership
   * rule rests on this column staying honest.
   */
  @NotNull
  @Column(nullable = false, updatable = false)
  private Integer userId;

  @NotNull
  @Column(nullable = false)
  private Integer issueId;

  @NotBlank
  @Column(nullable = false, columnDefinition = "text")
  private String content;

  /** Null while the comment is live; the moment it was dropped once it is not. */
  private OffsetDateTime deletedAt;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  /** Carries real meaning here: it is the only way to tell an edited comment from an original. */
  @UpdateTimestamp private OffsetDateTime updatedAt;
}
