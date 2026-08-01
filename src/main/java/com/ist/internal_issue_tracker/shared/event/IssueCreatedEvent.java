package com.ist.internal_issue_tracker.shared.event;

import java.time.OffsetDateTime;

/**
 * Published when an issue is filed. The first row of that issue's history, and the point every lead
 * time is measured from.
 *
 * <p><b>Delivered asynchronously</b>, unlike the {@code *DeactivatedEvent} records next to it. Those
 * are consumed inside the publisher's transaction because a stale roster must never be readable for
 * even an instant; an activity row has no such window - nothing reads it to make a decision. Being
 * asynchronous is what keeps a fault in the audit path from turning a successful issue write into a
 * 500, and Spring Modulith's {@code event_publication} registry is what keeps the event from being
 * lost in exchange.
 *
 * <p>That asynchrony is also why {@code occurredAt} is carried rather than taken by the consumer.
 * The listener runs after the transaction commits, on another thread, at a moment that has nothing
 * to do with when the change happened. Every metric built on this log is a difference between two of
 * these timestamps, so a consumer-side clock reading would feed queue latency straight into cycle
 * time - and, after a restart replays outstanding publications, could be off by hours.
 *
 * <p>{@code actorId} is whoever filed the issue, which here is also its reporter. The two coincide
 * only on creation: every later event carries the person making that particular change, who is
 * generally neither the reporter nor the assignee.
 *
 * <p>{@code projectId} is carried so the consumer never has to ask {@code issue} which project an
 * issue belongs to. It is the same denormalisation {@code issue_activities.project_id} makes, for
 * the same reason, and it is safe for the same reason: an issue does not move between projects.
 */
public record IssueCreatedEvent(
    Integer issueId, Integer projectId, Integer actorId, OffsetDateTime occurredAt) {}
