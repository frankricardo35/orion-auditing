package io.orion.audit.core.spi;

import io.orion.audit.core.model.AuditAction;

/**
 * Determines whether an entity change should be audited.
 */
public interface AuditFilter {

    /**
     * @param entityClass entity type
     * @param action audit action
     * @return {@code true} when the action should be recorded
     */
    boolean shouldAudit(Class<?> entityClass, AuditAction action);
}
