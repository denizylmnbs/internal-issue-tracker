package com.ist.internal_issue_tracker.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record UserCreateRequest(

        @NotBlank(message = "Name cannot be blank")
        @Size(min = 2, max = 255)
        String name,

        @NotBlank(message = "Surname cannot be blank")
        @Size(min = 2, max = 255)
        String surname,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email is not valid")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
    public UserCreateRequest {
        email = normalizeEmail(email);
    }

    // The DB unique index on email is case-sensitive, so we normalize here to
    // keep uniqueness checks and storage case-insensitive.
    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
