/**
 * Accounts, roles and credentials - the only module that reads {@code users}. Everyone else asks
 * through {@code UserLookup} or {@code CredentialsVerifier}, and hears about deletes through {@code
 * UserDeactivatedEvent}.
 */
@ApplicationModule(allowedDependencies = "shared")
package com.ist.internal_issue_tracker.user;

import org.springframework.modulith.ApplicationModule;
