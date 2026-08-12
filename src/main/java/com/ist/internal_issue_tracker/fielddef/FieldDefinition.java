package com.ist.internal_issue_tracker.fielddef;

import com.ist.internal_issue_tracker.shared.port.FieldKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * One user-defined value of one {@link FieldKind} - a single status, type, priority, unit or team
 * field a project (or, for the two global kinds, the whole instance) has available.
 *
 * <p>{@code projectId} null means this is a global row: either the actual value for {@link
 * FieldKind#PROJECT_STATUS}/{@link FieldKind#TEAM_FIELD}, which are not project-scoped at all, or
 * a seed template for the six kinds that are - copied into a project's own rows once, by {@link
 * FieldDefinitionProvisioningAdapter#seedDefaults}, and never read from again after that.
 *
 * <p>{@code code} is what every consuming module stores on its own entities and in the activity
 * log, and it is treated as immutable once created - {@code FieldDefinitionService} never updates
 * it. Everything else on the row ({@code label}, {@code color}, {@code sortOrder}, the four
 * semantic flags) is free to change without touching a single stored issue/sprint/epic/project.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "field_definitions")
public class FieldDefinition {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private FieldKind kind;

  /** Null for a global row - see the class Javadoc. */
  private Integer projectId;

  @NotBlank
  @Size(min = 1, max = 30)
  @Column(nullable = false, length = 30, updatable = false)
  private String code;

  @NotBlank
  @Size(min = 1, max = 100)
  @Column(nullable = false, length = 100)
  private String label;

  @Size(max = 7)
  @Column(length = 7)
  private String color;

  @NotNull
  @Column(nullable = false)
  private Integer sortOrder = 0;

  @NotNull
  @Column(nullable = false)
  private Boolean isActive = true;

  /** The code new rows of this kind/project default to when none is given. */
  @NotNull
  @Column(nullable = false)
  private Boolean isDefault = false;

  /** Work carrying this code is delivered - counted as completed throughput. */
  @NotNull
  @Column(nullable = false)
  private Boolean isDone = false;

  /** Work carrying this code left the flow without being delivered - excluded from throughput. */
  @NotNull
  @Column(nullable = false)
  private Boolean isCancelled = false;

  /** Work carrying this code is actively being progressed - what flow efficiency divides by. */
  @NotNull
  @Column(nullable = false)
  private Boolean isActiveWork = false;

  /** For {@link FieldKind#ISSUE_TYPE}: this type counts as a defect. */
  @NotNull
  @Column(nullable = false)
  private Boolean isDefect = false;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp private OffsetDateTime updatedAt;
}
