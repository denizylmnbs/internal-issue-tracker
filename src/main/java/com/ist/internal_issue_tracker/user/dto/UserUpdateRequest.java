package com.ist.internal_issue_tracker.user.dto;

import com.ist.internal_issue_tracker.user.internal.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 2, max = 255) String name,
    @NotBlank(message = "Surname cannot be blank") @Size(min = 2, max = 255) String surname,
    @NotBlank(message = "Email cannot be blank") @Email(message = "Email is not valid")
        String email) {

  public UserUpdateRequest {
    email = EmailNormalizer.normalize(email);
  }
}
