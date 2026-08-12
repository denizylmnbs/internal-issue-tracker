/**
 * Comments on issues. Authorization follows the issue's project, reached through {@code
 * IssueLookup} and {@code ProjectLookup}.
 */
@ApplicationModule(allowedDependencies = "shared")
package com.ist.internal_issue_tracker.comment;

import org.springframework.modulith.ApplicationModule;
