package io.orion.audit.core.query;

import io.orion.audit.core.model.AuditAction;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Public read model for stored audit data.
 *
 * @param id entry id
 * @param action audit action
 * @param entityType fully-qualified entity type
 * @param entityName display entity name
 * @param entityId entity id as a string
 * @param oldValues previous values
 * @param newValues current values
 * @param changedFields changed field payload
 * @param actorId actor id
 * @param actorName actor name
 * @param actorType actor type
 * @param ipAddress request IP address
 * @param userAgent request user agent
 * @param requestUri request URI
 * @param httpMethod HTTP method
 * @param traceId trace id
 * @param source source of the action
 * @param tenantId tenant id
 * @param tags entry tags
 * @param createdAt timestamp when the audit entry was created
 */
public record AuditRecord(
    String id,
    AuditAction action,
    String entityType,
    String entityName,
    String entityId,
    Map<String, Object> oldValues,
    Map<String, Object> newValues,
    Map<String, Object> changedFields,
    String actorId,
    String actorName,
    String actorType,
    String ipAddress,
    String userAgent,
    String requestUri,
    String httpMethod,
    String traceId,
    String source,
    String tenantId,
    List<String> tags,
    Instant createdAt
) {
}
