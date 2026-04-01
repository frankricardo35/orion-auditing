package io.orion.audit.autoconfigure.support;

import io.orion.audit.autoconfigure.driver.AuditLogEntity;
import io.orion.audit.core.model.AuditAction;
import io.orion.audit.core.query.AuditRecord;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps stored audit rows to the public read model.
 */
public class AuditRecordMapper {

    private final AuditJsonSupport jsonSupport;

    public AuditRecordMapper(AuditJsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    public AuditRecord map(AuditLogEntity entity) {
        return new AuditRecord(
            entity.getId(),
            AuditAction.valueOf(entity.getAction()),
            entity.getEntityType(),
            entity.getEntityName(),
            entity.getEntityId(),
            jsonSupport.readMap(entity.getOldValues()),
            jsonSupport.readMap(entity.getNewValues()),
            jsonSupport.readMap(entity.getChangedFields()),
            entity.getActorId(),
            entity.getActorName(),
            entity.getActorType(),
            entity.getIpAddress(),
            entity.getUserAgent(),
            entity.getRequestUri(),
            entity.getHttpMethod(),
            entity.getTraceId(),
            entity.getSource(),
            entity.getTenantId(),
            jsonSupport.readStringList(entity.getTags()),
            entity.getCreatedAt()
        );
    }

    public AuditRecord map(ResultSet rs) throws SQLException {
        return new AuditRecord(
            rs.getString("id"),
            AuditAction.valueOf(rs.getString("action")),
            rs.getString("entity_type"),
            rs.getString("entity_name"),
            rs.getString("entity_id"),
            jsonSupport.readMap(rs.getString("old_values")),
            jsonSupport.readMap(rs.getString("new_values")),
            jsonSupport.readMap(rs.getString("changed_fields")),
            rs.getString("actor_id"),
            rs.getString("actor_name"),
            rs.getString("actor_type"),
            rs.getString("ip_address"),
            rs.getString("user_agent"),
            rs.getString("request_uri"),
            rs.getString("http_method"),
            rs.getString("trace_id"),
            rs.getString("source"),
            rs.getString("tenant_id"),
            jsonSupport.readStringList(rs.getString("tags")),
            rs.getTimestamp("created_at").toInstant()
        );
    }
}
