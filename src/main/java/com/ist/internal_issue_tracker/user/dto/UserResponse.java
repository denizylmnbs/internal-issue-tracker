package com.ist.internal_issue_tracker.user.dto;

import com.ist.internal_issue_tracker.shared.security.Role;
import java.time.OffsetDateTime;

public record UserResponse(
    Integer id,
    String name,
    String surname,
    String email,
    Role role,
    Boolean isActive,
    OffsetDateTime createdAt,
    // Null when the user has no avatar - the frontend's AvatarFallback (initials) is the
    // contract for that case. When present this is a presigned URL with its own expiry (see
    // ObjectUrlSigner); never persist or cache it beyond the response it came in.
    String avatarUrl) {}
