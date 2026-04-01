package io.orion.audit.autoconfigure.driver;

import io.orion.audit.core.model.AuditEntry;
import io.orion.audit.core.spi.ValueSerializer;

/**
 * Maps the public audit model to the database representation.
 */
public class AuditLogEntityMapper {

    private final ValueSerializer valueSerializer;

    public AuditLogEntityMapper(ValueSerializer valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    public AuditLogEntity map(AuditEntry entry) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(entry.getId());
        entity.setAction(entry.getAction().name());
        entity.setEntityType(entry.getEntityType());
        entity.setEntityName(entry.getEntityName());
        entity.setEntityId(entry.getEntityId());
        entity.setOldValues(valueSerializer.serialize(entry.getOldValues()));
        entity.setNewValues(valueSerializer.serialize(entry.getNewValues()));
        entity.setChangedFields(valueSerializer.serialize(entry.getChangedFields()));
        entity.setActorId(entry.getActorId());
        entity.setActorName(entry.getActorName());
        entity.setActorType(entry.getActorType());
        entity.setIpAddress(entry.getIpAddress());
        entity.setUserAgent(entry.getUserAgent());
        entity.setRequestUri(entry.getRequestUri());
        entity.setHttpMethod(entry.getHttpMethod());
        entity.setTraceId(entry.getTraceId());
        entity.setSource(entry.getSource());
        entity.setTenantId(entry.getTenantId());
        entity.setTags(valueSerializer.serialize(entry.getTags()));
        entity.setCreatedAt(entry.getCreatedAt());
        return entity;
    }
}
