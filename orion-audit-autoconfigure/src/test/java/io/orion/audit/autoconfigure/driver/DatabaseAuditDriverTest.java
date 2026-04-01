package io.orion.audit.autoconfigure.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.autoconfigure.support.JacksonValueSerializer;
import io.orion.audit.core.model.AuditAction;
import io.orion.audit.core.model.AuditEntry;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseAuditDriverTest {

    @Test
    void shouldMapAndPersistUsingRepositoryForDefaultTable() {
        AtomicReference<AuditLogEntity> saved = new AtomicReference<>();
        AuditLogRepository repository = repositoryStub(saved);
        AuditLogEntityMapper mapper = new AuditLogEntityMapper(new JacksonValueSerializer(new ObjectMapper()));
        DatabaseAuditDriver driver = new DatabaseAuditDriver(repository, mapper, jdbcTemplate(), new AuditProperties());
        Map<String, Object> nameChange = new LinkedHashMap<>();
        nameChange.put("old", null);
        nameChange.put("new", "Alice");

        driver.save(AuditEntry.builder()
            .id("1")
            .action(AuditAction.INSERT)
            .entityType("io.orion.Customer")
            .entityName("Customer")
            .entityId("99")
            .oldValues(Map.of())
            .newValues(Map.of("name", "Alice"))
            .changedFields(Map.of("name", nameChange))
            .tags(List.of("Customer", "insert"))
            .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
            .build());

        assertThat(saved.get()).isNotNull();
        assertThat(saved.get().getAction()).isEqualTo("INSERT");
        assertThat(saved.get().getNewValues()).contains("Alice");
    }

    @Test
    void shouldMapJsonFields() {
        AuditLogEntityMapper mapper = new AuditLogEntityMapper(new JacksonValueSerializer(new ObjectMapper()));

        AuditLogEntity entity = mapper.map(AuditEntry.builder()
            .id("1")
            .action(AuditAction.UPDATE)
            .entityType("type")
            .entityName("name")
            .entityId("42")
            .oldValues(Map.of("name", "before"))
            .newValues(Map.of("name", "after"))
            .changedFields(Map.of("name", Map.of("old", "before", "new", "after")))
            .tags(List.of("Customer", "update"))
            .createdAt(Instant.now())
            .build());

        assertThat(entity.getOldValues()).contains("before");
        assertThat(entity.getNewValues()).contains("after");
        assertThat(entity.getChangedFields()).contains("before");
        assertThat(entity.getTags()).contains("update");
    }

    @Test
    void shouldCastJsonColumnsForPostgresqlCustomTableSql() {
        String sql = DatabaseAuditDriver.buildInsertSql("public.audit_logs", true);

        assertThat(sql).contains("CAST(:oldValues AS jsonb)");
        assertThat(sql).contains("CAST(:newValues AS jsonb)");
        assertThat(sql).contains("CAST(:changedFields AS jsonb)");
        assertThat(sql).contains("CAST(:tags AS jsonb)");
    }

    @Test
    void shouldKeepPlainJsonBindingsForNonPostgresqlSql() {
        String sql = DatabaseAuditDriver.buildInsertSql("audit_log", false);

        assertThat(sql).contains(":oldValues");
        assertThat(sql).contains(":newValues");
        assertThat(sql).contains(":changedFields");
        assertThat(sql).contains(":tags");
        assertThat(sql).doesNotContain("CAST(:oldValues AS jsonb)");
    }

    private AuditLogRepository repositoryStub(AtomicReference<AuditLogEntity> saved) {
        return (AuditLogRepository) Proxy.newProxyInstance(
            AuditLogRepository.class.getClassLoader(),
            new Class<?>[]{AuditLogRepository.class},
            (proxy, method, args) -> {
                if ("save".equals(method.getName())) {
                    AuditLogEntity entity = (AuditLogEntity) args[0];
                    saved.set(entity);
                    return entity;
                }
                if ("toString".equals(method.getName())) {
                    return "AuditLogRepositoryStub";
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                }
                throw new UnsupportedOperationException("Unsupported repository method: " + method.getName());
            }
        );
    }

    private NamedParameterJdbcTemplate jdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:driver-test;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
