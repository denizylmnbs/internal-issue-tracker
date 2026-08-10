package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;
import org.springframework.modulith.events.Externalized;

/**
 * Published when a project is soft-deleted, for the activity log.
 *
 * <p><b>Not a replacement for {@link ProjectDeactivatedEvent}, which is published alongside it.</b>
 * The two describe the same moment and are deliberately separate, because their consumers need
 * opposite things. The deactivation is consumed inline, inside the deleting transaction, so that a
 * dead project's memberships are retired before anyone can read them; this one is consumed after the
 * commit, on another thread, so that a fault in the audit path cannot fail the delete. One event
 * cannot be delivered both ways, and collapsing them would mean giving up one of the two guarantees.
 */
@Externalized("project-events::#{#this.projectId().toString()}")
public record ProjectDeletedEvent(Integer projectId, Integer actorId, OffsetDateTime occurredAt) {}
