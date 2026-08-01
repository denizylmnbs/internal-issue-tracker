/**
 * Login and token issuing. Owns no table: it verifies through {@code CredentialsVerifier} and signs
 * through {@code JwtService}, which is why {@code shared} is enough for it too.
 */
@ApplicationModule(allowedDependencies = "shared")
package com.ist.internal_issue_tracker.auth;

import org.springframework.modulith.ApplicationModule;
