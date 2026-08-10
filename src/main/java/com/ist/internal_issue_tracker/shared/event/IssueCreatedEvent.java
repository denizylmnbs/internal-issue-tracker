package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;
import org.springframework.modulith.events.Externalized;

/**
 * Published when an issue is filed. The first row of that issue's history, and the point every lead
 * time is measured from.
 *
 * <p><b>Delivered over Kafka</b>, unlike the {@code *DeactivatedEvent} records next to it. Those are
 * consumed inside the publisher's transaction because a stale roster must never be readable for even
 * an instant; an activity row has no such window - nothing reads it to make a decision. Going out to
 * a broker is what keeps a fault in the audit path from turning a successful issue write into a 500,
 * and what lets the {@code activity} module be pulled out into its own service later without the
 * publisher learning anything about it.
 *
 * <p>Nothing is lost in exchange, because the send is not made from the transaction. The event is
 * written to {@code event_publication} in the same commit as the issue itself and forwarded to the
 * broker afterwards, so a broker that is down delays the message rather than dropping it - and a
 * message that never reaches the broker leaves a row behind that says so. The registry is an outbox
 * now rather than a delivery queue; see {@code spring.modulith.events.*} in application.properties.
 *
 * <p>That asynchrony is also why {@code occurredAt} is carried rather than taken by the consumer.
 * The listener runs after the transaction commits, in another thread and possibly in another
 * process, at a moment that has nothing to do with when the change happened. Every metric built on
 * this log is a difference between two of these timestamps, so a consumer-side clock reading would
 * feed queue latency straight into cycle time - and, after an offset is rewound and the topic
 * replayed, could be off by days.
 *
 * <p><b>This record is a published contract.</b> It is serialised to JSON on {@code issue-events}
 * and read by consumers that are not built from this source tree - possibly not from this
 * repository. A field may be added, because a reader that does not know it ignores it. A field may
 * not be removed, renamed, or have its type changed, and neither may the record itself move package:
 * its fully-qualified name travels in the {@code __TypeId__} header and is how a consumer picks the
 * class to deserialise into.
 *
 * <p>The routing key is the issue id, so every event about one issue lands on the same partition and
 * their order is preserved. Kafka guarantees order within a partition and nowhere else; keyed on
 * anything coarser, a consumer could see an issue deleted before it was created.
 *
 * <p>{@code actorId} is whoever filed the issue, which here is also its reporter. The two coincide
 * only on creation: every later event carries the person making that particular change, who is
 * generally neither the reporter nor the assignee.
 *
 * <p>{@code projectId} is carried so the consumer never has to ask {@code issue} which project an
 * issue belongs to. It is the same denormalisation {@code issue_activities.project_id} makes, for
 * the same reason, and it is safe for the same reason: an issue does not move between projects.
 *
 * <p>{@code dimensions} is the same idea applied to the attributes that <em>can</em> change - see
 * {@link IssueDimensions}. On this event they describe the issue as it was filed, which is what makes
 * "how many bugs were opened this week" answerable without reading {@code issues}.
 */
@Externalized("issue-events::#{#this.issueId().toString()}")
public record IssueCreatedEvent(
    Integer issueId,
    Integer projectId,
    Integer actorId,
    OffsetDateTime occurredAt,
    IssueDimensions dimensions) {}
