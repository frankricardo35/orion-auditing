package io.orion.audit.autoconfigure.support;

import io.orion.audit.autoconfigure.properties.AuditProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Utility for building schema-qualified audit table references.
 */
public final class AuditJdbcSupport {

    private AuditJdbcSupport() {
    }

    public static String qualifiedTableName(AuditProperties properties) {
        if (properties.getSchema() == null || properties.getSchema().isBlank()) {
            return properties.getTableName();
        }
        return properties.getSchema() + "." + properties.getTableName();
    }

    public static Optional<String> databaseProductName(DataSource dataSource) {
        if (dataSource == null) {
            return Optional.empty();
        }
        try (Connection connection = dataSource.getConnection()) {
            return Optional.ofNullable(connection.getMetaData().getDatabaseProductName());
        }
        catch (SQLException ex) {
            return Optional.empty();
        }
    }
}
