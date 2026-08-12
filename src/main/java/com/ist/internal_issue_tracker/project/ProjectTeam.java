package com.ist.internal_issue_tracker.project;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A whole team assigned to a project. Everyone in the team counts as a member of the project for as
 * long as this row is active.
 *
 * <p>Owned by {@code project}, not {@code team}: a team has no business knowing which projects it
 * has been put on, while a project very much needs to know which teams it works with. {@code
 * teamId} is a plain id validated through {@code TeamLookup}, which keeps the dependency one-way
 * and out of the type system entirely.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "project_teams")
public class ProjectTeam {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false)
  private Integer projectId;

  @Column(nullable = false)
  private Integer teamId;

  @Column(nullable = false)
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp private OffsetDateTime updatedAt;
}
