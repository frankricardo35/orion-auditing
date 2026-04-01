package io.orion.audit.autoconfigure.support;

import java.lang.reflect.Field;

/**
 * Descriptor for a single auditable field.
 *
 * @param field backing field
 * @param sourceName entity field name
 * @param auditName name used in audit payloads
 */
public record AuditFieldDescriptor(Field field, String sourceName, String auditName) {
}
