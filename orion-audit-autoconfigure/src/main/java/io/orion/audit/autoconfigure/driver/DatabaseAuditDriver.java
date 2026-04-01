package io.orion.audit.autoconfigure.driver;

import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.autoconfigure.support.AuditJdbcSupport;
import io.orion.audit.core.model.AuditEntry;
import io.orion.audit.core.spi.AuditDriver;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Timestamp;

/**
 * Default database-backed audit driver.
 */
public class DatabaseAuditDriver implements AuditDriver {

    private final AuditLogRepository repository;
    private final AuditLogEntityMapper mapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AuditProperties properties;
    private final String insertSql;

    public DatabaseAuditDriver(
        AuditLogRepository repository,
        AuditLogEntityMapper mapper,
        NamedParameterJdbcTemplate jdbcTemplate,
        AuditProperties properties
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.insertSql = buildInsertSql(qualifiedTableName(), isPostgreSql(jdbcTemplate));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditEntry entry) {
        AuditLogEntity entity = mapper.map(entry);
        if (usesDefaultTable() && repository != null) {
            repository.save(entity);
            return;
        }

        jdbcTemplate.update(insertSql, parameters(entity));
    }

    private boolean usesDefaultTable() {
        return "audit_log".equals(properties.getTableName()) && (properties.getSchema() == null || properties.getSchema().isBlank());
    }

    private String qualifiedTableName() {
        return AuditJdbcSupport.qualifiedTableName(properties);
    }

    static String buildInsertSql(String qualifiedTableName, boolean postgreSql) {
        String oldValuesExpression = postgreSql ? "CAST(:oldValues AS jsonb)" : ":oldValues";
        String newValuesExpression = postgreSql ? "CAST(:newValues AS jsonb)" : ":newValues";
        String changedFieldsExpression = postgreSql ? "CAST(:changedFields AS jsonb)" : ":changedFields";
        String tagsExpression = postgreSql ? "CAST(:tags AS jsonb)" : ":tags";
        return """
            INSERT INTO %s (
                id, action, entity_type, entity_name, entity_id, old_values, new_values, changed_fields,
                actor_id, actor_name, actor_type, ip_address, user_agent, request_uri, http_method,
                trace_id, source, tenant_id, tags, created_at
            ) VALUES (
                :id, :action, :entityType, :entityName, :entityId, %s, %s, %s,
                :actorId, :actorName, :actorType, :ipAddress, :userAgent, :requestUri, :httpMethod,
                :traceId, :source, :tenantId, %s, :createdAt
            )
            """.formatted(
            qualifiedTableName,
            oldValuesExpression,
            newValuesExpression,
            changedFieldsExpression,
            tagsExpression
        );
    }

    private static boolean isPostgreSql(NamedParameterJdbcTemplate jdbcTemplate) {
        DataSource dataSource = jdbcTemplate.getJdbcTemplate().getDataSource();
        return AuditJdbcSupport.databaseProductName(dataSource)
            .map(name -> name.toLowerCase().contains("postgresql"))
            .orElse(false);
    }

    private MapSqlParameterSource parameters(AuditLogEntity entity) {
        return new MapSqlParameterSource()
            .addValue("id", entity.getId())
            .addValue("action", entity.getAction())
            .addValue("entityType", entity.getEntityType())
            .addValue("entityName", entity.getEntityName())
            .addValue("entityId", entity.getEntityId())
            .addValue("oldValues", entity.getOldValues())
            .addValue("newValues", entity.getNewValues())
            .addValue("changedFields", entity.getChangedFields())
            .addValue("actorId", entity.getActorId())
            .addValue("actorName", entity.getActorName())
            .addValue("actorType", entity.getActorType())
            .addValue("ipAddress", entity.getIpAddress())
            .addValue("userAgent", entity.getUserAgent())
            .addValue("requestUri", entity.getRequestUri())
            .addValue("httpMethod", entity.getHttpMethod())
            .addValue("traceId", entity.getTraceId())
            .addValue("source", entity.getSource())
            .addValue("tenantId", entity.getTenantId())
            .addValue("tags", entity.getTags())
            .addValue("createdAt", entity.getCreatedAt() == null ? null : Timestamp.from(entity.getCreatedAt()));
    }
}
