package com.ist.internal_issue_tracker.team;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "teams")
public class Team {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotBlank
  @Column(nullable = false, unique = true)
  @Size(min = 2, max = 255)
  private String name;

  /** Optional - a team is not required to declare a discipline. */
  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  private TeamField field;

  /**
   * Plain user id rather than a {@code @ManyToOne User}: an association would pull a JPA dependency
   * across the module boundary into {@code user} and break {@code ModularityTests}. The referenced
   * user is validated in the service layer; the foreign key is enforced by the database.
   */
  @NotNull
  @Column(nullable = false)
  private Integer leaderId;

  @NotNull
  @Column(nullable = false)
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp private OffsetDateTime updatedAt;
}
