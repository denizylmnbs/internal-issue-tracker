/**
 * The only module every other one is allowed to depend on, and the only one that depends on none.
 *
 * <p>It holds the three things a business module cannot own without dragging its neighbours in: the
 * {@code port} interfaces a module implements so others can ask it questions without naming its
 * types, the {@code event} records a module publishes so others can react to its deletes, and the
 * cross-cutting {@code web}, {@code security} and {@code exception} plumbing every controller needs.
 *
 * <p>{@code OPEN} rather than the default: its sub-packages are meant to be imported directly, so
 * treating them as internals would mean declaring a named interface for each one to say the same
 * thing. Every other module is closed and declares {@code allowedDependencies = "shared"}, which is
 * what keeps the dependency graph a star rather than a web - and what makes a stray import of
 * another module's service fail {@code ModularityTests} instead of quietly compiling.
 *
 * <p>What that check cannot see is a JPQL or native query naming another module's entity or table,
 * because a query is a string. Those are kept out by giving a module no reason to write one - see
 * {@code TeamLookup#activeTeamIdsOfUser} for the pattern - and where one is knowingly kept, it is
 * called out in the repository that holds it.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.ist.internal_issue_tracker.shared;

import org.springframework.modulith.ApplicationModule;
