package io.orion.audit.autoconfigure.config;

import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.autoconfigure.support.AuditJdbcSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.SmartInitializingSingleton;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Creates the audit table automatically when the application starts and Flyway did not create it.
 */
public class AuditSchemaInitializer implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(AuditSchemaInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final AuditProperties properties;

    public AuditSchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate, AuditProperties properties) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.isEnabled() || !properties.isDatabaseDriverEnabled() || !properties.isInitializeSchema()) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            if (tableExists(connection)) {
                return;
            }
            DatabaseVendor vendor = DatabaseVendor.from(connection.getMetaData());
            createSchemaIfNecessary(vendor);
            createTable(vendor);
            createIndexes(vendor, connection);
            log.info("Initialized audit table {}", AuditJdbcSupport.qualifiedTableName(properties));
        }
        catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize audit schema", ex);
        }
    }

    private boolean tableExists(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String schema = normalizedSchema(connection);
        try (ResultSet tables = metaData.getTables(connection.getCatalog(), schema, properties.getTableName(), new String[]{"TABLE"})) {
            if (tables.next()) {
                return true;
            }
        }
        try (ResultSet tables = metaData.getTables(connection.getCatalog(), schema, properties.getTableName().toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
            if (tables.next()) {
                return true;
            }
        }
        try (ResultSet tables = metaData.getTables(connection.getCatalog(), schema, properties.getTableName().toLowerCase(Locale.ROOT), new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private void createSchemaIfNecessary(DatabaseVendor vendor) {
        if (properties.getSchema() == null || properties.getSchema().isBlank()) {
            return;
        }
        jdbcTemplate.execute("create schema if not exists " + properties.getSchema());
    }

    private void createTable(DatabaseVendor vendor) {
        Map<String, String> jsonTypes = new LinkedHashMap<>();
        jsonTypes.put("old_values", vendor.jsonType(properties.isPreferJsonColumn()));
        jsonTypes.put("new_values", vendor.jsonType(properties.isPreferJsonColumn()));
        jsonTypes.put("changed_fields", vendor.jsonType(properties.isPreferJsonColumn()));
        jsonTypes.put("tags", vendor.jsonType(properties.isPreferJsonColumn()));

        jdbcTemplate.execute("""
            create table %s (
                id varchar(36) primary key,
                action varchar(32) not null,
                entity_type varchar(255) not null,
                entity_name varchar(255) not null,
                entity_id varchar(255),
                old_values %s,
                new_values %s,
                changed_fields %s,
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
                tags %s,
                created_at %s not null default %s
            )
            """.formatted(
            AuditJdbcSupport.qualifiedTableName(properties),
            jsonTypes.get("old_values"),
            jsonTypes.get("new_values"),
            jsonTypes.get("changed_fields"),
            jsonTypes.get("tags"),
            vendor.timestampType(),
            vendor.currentTimestampExpression()
        ));
    }

    private void createIndexes(DatabaseVendor vendor, Connection connection) throws SQLException {
        createIndexIfMissing(connection, indexName("entity_type"), "entity_type");
        createIndexIfMissing(connection, indexName("entity_id"), "entity_id");
        createIndexIfMissing(connection, indexName("action"), "action");
        createIndexIfMissing(connection, indexName("actor_type"), "actor_type");
        createIndexIfMissing(connection, indexName("actor_id"), "actor_id");
        createIndexIfMissing(connection, indexName("created_at"), "created_at");
    }

    private void createIndexIfMissing(Connection connection, String indexName, String columnName) throws SQLException {
        if (indexExists(connection, indexName)) {
            return;
        }
        jdbcTemplate.execute("create index " + indexName + " on " + AuditJdbcSupport.qualifiedTableName(properties) + " (" + columnName + ")");
    }

    private boolean indexExists(Connection connection, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String schema = normalizedSchema(connection);
        try (ResultSet indexes = metaData.getIndexInfo(connection.getCatalog(), schema, properties.getTableName(), false, false)) {
            while (indexes.next()) {
                String existingIndex = indexes.getString("INDEX_NAME");
                if (existingIndex != null && existingIndex.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalizedSchema(Connection connection) throws SQLException {
        if (properties.getSchema() != null && !properties.getSchema().isBlank()) {
            return properties.getSchema();
        }
        String schema = connection.getSchema();
        return schema == null || schema.isBlank() ? null : schema;
    }

    private String indexName(String suffix) {
        return "idx_" + properties.getTableName() + "_" + suffix;
    }

    private enum DatabaseVendor {
        POSTGRESQL,
        MYSQL,
        H2,
        OTHER;

        static DatabaseVendor from(DatabaseMetaData metaData) throws SQLException {
            String name = metaData.getDatabaseProductName();
            if (name == null) {
                return OTHER;
            }
            String normalized = name.toLowerCase(Locale.ROOT);
            if (normalized.contains("postgres")) {
                return POSTGRESQL;
            }
            if (normalized.contains("mysql") || normalized.contains("mariadb")) {
                return MYSQL;
            }
            if (normalized.contains("h2")) {
                return H2;
            }
            return OTHER;
        }

        String jsonType(boolean preferJsonColumn) {
            if (!preferJsonColumn) {
                return "clob";
            }
            return switch (this) {
                case POSTGRESQL -> "jsonb";
                case MYSQL -> "json";
                case H2, OTHER -> "clob";
            };
        }

        String timestampType() {
            return this == POSTGRESQL ? "timestamp with time zone" : "timestamp";
        }

        String currentTimestampExpression() {
            return "current_timestamp";
        }
    }
}
