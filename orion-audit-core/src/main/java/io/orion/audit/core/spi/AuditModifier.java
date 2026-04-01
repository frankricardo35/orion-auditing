package io.orion.audit.core.spi;

import io.orion.audit.core.model.AuditEntry;

/**
 * Allows applications to enrich or transform an audit entry before it is stored.
 */
public interface AuditModifier {

    /**
     * Returns the audit entry to persist after applying modifications.
     *
     * @param entry entry prepared by the audit orchestrator
     * @return entry to persist
     */
    AuditEntry modify(AuditEntry entry);
}
