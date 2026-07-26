package com.ist.internal_issue_tracker.user.internal;

import java.util.Locale;

/** The DB unique index on email is case-sensitive, so requests normalize here to
 * keep uniqueness checks and storage case-insensitive. */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
