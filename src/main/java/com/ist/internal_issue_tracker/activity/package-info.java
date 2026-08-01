/**
 * The history of what changed, who changed it and when - the only module that stores time rather
 * than state, and the one every agile metric is computed from.
 *
 * <p>It writes nothing of its own accord. Every row here is the record of an event another module
 * published, consumed asynchronously, so no business module knows this log exists and none of them
 * can be brought down by a fault in it. The events are the whole of the coupling: {@code issue}
 * names an {@code IssueField}, this module decides that becomes an {@code IssueActionType}.
 *
 * <p>The metrics live in {@code activity.metrics} rather than in a module of their own. They are a
 * read model over these tables, and the alternative - a separate module reading them - would mean
 * either a port on {@code shared} per metric or a native query naming another module's table, which
 * is the one violation {@code ModularityTests} cannot see. If they ever need to be consumed from
 * outside, {@code @NamedInterface} opens them without moving them.
 *
 * <p>Known gap: {@code user_id} is {@code NOT NULL} on all three tables, so every row needs a human
 * behind it. That holds today because every write path carries an authenticated caller. A change
 * made by the system itself - a cleanup listener retiring rows on delete, say - would have no actor
 * and no way to be recorded; it would need either a synthetic user or a nullable column, and the
 * choice should be made when the first such change appears rather than guessed at now.
 */
@ApplicationModule(allowedDependencies = "shared")
package com.ist.internal_issue_tracker.activity;

import org.springframework.modulith.ApplicationModule;
