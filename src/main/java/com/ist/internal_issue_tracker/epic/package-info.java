/**
 * Epics, grouping issues within a project. Reaches the project and its participants through ports;
 * its own rows carry plain ids rather than associations for the same reason.
 */
@ApplicationModule(allowedDependencies = "shared")
package com.ist.internal_issue_tracker.epic;

import org.springframework.modulith.ApplicationModule;
