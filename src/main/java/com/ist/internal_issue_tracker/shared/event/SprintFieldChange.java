package com.ist.internal_issue_tracker.shared.event;

/**
 * One sprint field moving, under the same value rules as {@link IssueFieldChange}: a readable
 * rendering that fits 255 characters, or null where there is none.
 *
 * <p>{@code DATES} renders both dates at once, as {@code start..end}, with the end left off when the
 * sprint has no end date. One action type covers the pair, so one value has to describe the pair.
 */
public record SprintFieldChange(SprintField field, String oldValue, String newValue) {}
