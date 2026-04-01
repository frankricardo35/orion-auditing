package io.orion.audit.autoconfigure.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.autoconfigure.support.AuditJsonSupport;
import io.orion.audit.autoconfigure.support.AuditRecordMapper;
import io.orion.audit.core.model.AuditAction;
import io.orion.audit.core.query.AuditPageRequest;
import io.orion.audit.core.query.AuditPageResult;
import io.orion.audit.core.query.AuditQueryCriteria;
import io.orion.audit.core.query.AuditRecord;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseAuditQueryServiceTest {

    @Test
    void shouldFilterAndSortAuditRecords() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("""
            create table audit_log (
                id varchar(36) primary key,
                action varchar(32) not null,
                entity_type varchar(255) not null,
                entity_name varchar(255) not null,
                entity_id varchar(255),
                old_values clob,
                new_values clob,
                changed_fields clob,
                actor_id varchar(255),
                actor_name varchar(255),
                actor_type varchar(255),
                ip_address varchar(64),
                user_agent varchar(1024),
                request_uri varchar(1024),
                http_method varchar(32),
                trace_id varchar(255),
                source varchar(128),
                tenant_id varchar(255),
                tags clob,
                created_at timestamp not null
            )
            """);
        jdbcTemplate.getJdbcTemplate().update("""
            insert into audit_log (id, action, entity_type, entity_name, entity_id, old_values, new_values, changed_fields, actor_id, actor_type, source, tenant_id, tags, created_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, "1", "INSERT", "demo.Customer", "Customer", "7", "{}", "{\"name\":\"Alice\"}", "{\"name\":{\"old\":null,\"new\":\"Alice\"}}", "actor-1", "users", "api", "tenant-1", "[\"Customer\"]", java.sql.Timestamp.from(java.time.Instant.parse("2025-01-01T00:00:00Z")));
        jdbcTemplate.getJdbcTemplate().update("""
            insert into audit_log (id, action, entity_type, entity_name, entity_id, old_values, new_values, changed_fields, actor_id, actor_type, source, tenant_id, tags, created_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, "2", "UPDATE", "demo.Customer", "Customer", "7", "{\"name\":\"Alice\"}", "{\"name\":\"Alice Smith\"}", "{\"name\":{\"old\":\"Alice\",\"new\":\"Alice Smith\"}}", "actor-1", "users", "api", "tenant-1", "[\"Customer\"]", java.sql.Timestamp.from(java.time.Instant.parse("2025-01-02T00:00:00Z")));
        jdbcTemplate.getJdbcTemplate().update("""
            insert into audit_log (id, action, entity_type, entity_name, entity_id, old_values, new_values, changed_fields, actor_id, actor_type, source, tenant_id, tags, created_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, "3", "DELETE", "demo.Order", "Order", "8", "{\"status\":\"NEW\"}", "{}", "{\"status\":{\"old\":\"NEW\",\"new\":null}}", "actor-2", "customers", "batch", "tenant-2", "[\"Order\"]", java.sql.Timestamp.from(java.time.Instant.parse("2025-01-03T00:00:00Z")));

        DatabaseAuditQueryService service = new DatabaseAuditQueryService(
            null,
            jdbcTemplate,
            new AuditProperties(),
            new AuditRecordMapper(new AuditJsonSupport(new ObjectMapper()))
        );

        AuditPageResult<AuditRecord> result = service.find(
            AuditQueryCriteria.builder()
                .entityType("demo.Customer")
                .actorId("actor-1")
                .actorType("users")
                .tenantId("tenant-1")
                .actions(java.util.Set.of(AuditAction.INSERT, AuditAction.UPDATE))
                .build(),
            AuditPageRequest.of(0, 10)
        );

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).extracting(AuditRecord::id).containsExactly("2", "1");
        assertThat(result.content().get(0).changedFields()).containsKey("name");
    }

    private DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:query-test;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
