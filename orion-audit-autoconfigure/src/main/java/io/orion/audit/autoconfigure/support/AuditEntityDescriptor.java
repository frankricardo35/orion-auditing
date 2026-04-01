package io.orion.audit.autoconfigure.support;

import io.orion.audit.core.model.AuditAction;

import java.util.List;
import java.util.Set;

/**
 * Cached audit metadata for an entity type.
 */
public record AuditEntityDescriptor(
    Class<?> entityClass,
    String entityType,
    String entityName,
    Set<AuditAction> actions,
    boolean storeFullSnapshot,
    List<AuditFieldDescriptor> fields
) {

    public boolean supports(AuditAction action) {
        return actions.contains(action);
    }

    public AuditFieldDescriptor fieldBySourceName(String sourceName) {
        for (AuditFieldDescriptor field : fields) {
            if (field.sourceName().equals(sourceName)) {
                return field;
            }
        }
        return null;
    }
}
