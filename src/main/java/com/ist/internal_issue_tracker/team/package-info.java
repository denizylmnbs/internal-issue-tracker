/**
 * Teams and their rosters - the only module that reads {@code teams} and {@code team_users}.
 * Answers {@code TeamLookup} for everyone else and publishes {@code TeamDeactivatedEvent} when a
 * team goes.
 */
@ApplicationModule(allowedDependencies = "shared")
package com.ist.internal_issue_tracker.team;

import org.springframework.modulith.ApplicationModule;
