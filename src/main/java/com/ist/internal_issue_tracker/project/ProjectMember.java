package com.ist.internal_issue_tracker.project;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A user assigned to a project directly, as opposed to reaching it through {@link ProjectTeam}.
 * Both routes count as membership, so anything answering "who works on this project" has to read
 * both.
 *
 * <p>Lives in {@code project} rather than {@code user} for the same reason {@code TeamMember} lives
 * in {@code team}: the membership hangs off the project, and {@code userId} stays a plain id
 * validated through {@code UserLookup} so no dependency crosses into the {@code user} module.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "project_users")
public class ProjectMember {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false)
  private Integer projectId;

  @Column(nullable = false)
  private Integer userId;

  @Column(nullable = false)
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp private OffsetDateTime updatedAt;
}
