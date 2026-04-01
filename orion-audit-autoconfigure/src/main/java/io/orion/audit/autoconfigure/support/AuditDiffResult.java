package io.orion.audit.autoconfigure.support;

import java.util.Map;

/**
 * Diff result for entity snapshots.
 *
 * @param oldValues old values to persist
 * @param newValues new values to persist
 * @param changedFields changed field payload
 */
public record AuditDiffResult(
    Map<String, Object> oldValues,
    Map<String, Object> newValues,
    Map<String, Object> changedFields
) {
}
