/**
 * Projects, the users assigned to them directly, and the teams assigned to them. The two routes
 * together are what "works on this project" means, which is the question {@code ProjectLookup}
 * answers for every module that gates access on it.
 */
@ApplicationModule(allowedDependencies = "shared")
package com.ist.internal_issue_tracker.project;

import org.springframework.modulith.ApplicationModule;
