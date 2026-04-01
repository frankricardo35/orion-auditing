package io.orion.audit.core.spi;

import io.orion.audit.core.model.AuditEntry;

/**
 * Persistence abstraction for audit entries.
 */
public interface AuditDriver {

    /**
     * Persists the supplied audit entry.
     *
     * @param entry audit entry to store
     */
    void save(AuditEntry entry);
}
