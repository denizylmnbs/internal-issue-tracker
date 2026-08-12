package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.security.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Setter
@Getter
@NoArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotBlank
  @Column(nullable = false)
  @Size(min = 2, max = 255)
  private String name;

  @NotBlank
  @Column(nullable = false)
  @Size(min = 2, max = 255)
  private String surname;

  @NotBlank
  @Column(nullable = false, unique = true)
  @Email
  @Size(max = 255)
  private String email;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Setter(AccessLevel.NONE) // Blocks the setter from being used
  private Role role = Role.USER;

  @NotBlank
  @Column(nullable = false)
  @Size(max = 255)
  @Setter(AccessLevel.NONE) // Blocks the setter from being used
  private String passwordHashed;

  @NotNull
  @Column(nullable = false)
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp private OffsetDateTime updatedAt;

  public void changePassword(String newHashedPassword) {
    this.passwordHashed = newHashedPassword;
  }

  public void changeRole(Role newRole) {
    this.role = newRole;
  }
}
