/**
 * Issues - the module that points at the most others, and so the one where the ports earn their
 * keep: project, sprint, epic, user and team references are all validated through {@code
 * shared.port} rather than through associations that would tie five modules into one graph.
 */
@ApplicationModule(allowedDependencies = "shared")
package com.ist.internal_issue_tracker.issue;

import org.springframework.modulith.ApplicationModule;
