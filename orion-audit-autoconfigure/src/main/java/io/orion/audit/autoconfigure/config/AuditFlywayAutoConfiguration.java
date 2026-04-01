package io.orion.audit.autoconfigure.config;

import io.orion.audit.autoconfigure.properties.AuditProperties;
import org.flywaydb.core.api.Location;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds orion-audit migrations to Flyway.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(FlywayConfigurationCustomizer.class)
public class AuditFlywayAutoConfiguration {

    @Bean
    FlywayConfigurationCustomizer auditFlywayConfigurationCustomizer(AuditProperties properties, ObjectProvider<DataSource> dataSourceProvider) {
        return configuration -> {
            if (!properties.isDatabaseDriverEnabled() || !properties.getFlyway().isEnabled()) {
                return;
            }

            List<String> auditLocations = new ArrayList<>();
            String vendor = resolveVendor(properties, dataSourceProvider.getIfAvailable());
            if (vendor != null) {
                auditLocations.add("classpath:META-INF/orion-audit/db/migration/" + vendor);
            }
            auditLocations.addAll(properties.getFlyway().getLocations());

            List<String> finalLocations = new ArrayList<>();
            if (properties.getFlyway().isAppendToExisting()) {
                for (Location location : configuration.getLocations()) {
                    finalLocations.add(location.toString());
                }
            }
            finalLocations.addAll(auditLocations);
            configuration.locations(finalLocations.toArray(String[]::new));

            Map<String, String> placeholders = new LinkedHashMap<>(configuration.getPlaceholders());
            placeholders.put("orion_audit_table", properties.getTableName());
            placeholders.put("orion_audit_schema_prefix", schemaPrefix(properties.getSchema()));
            configuration.placeholders(placeholders);
        };
    }

    private String resolveVendor(AuditProperties properties, DataSource dataSource) {
        String configuredVendor = properties.getFlyway().getVendor();
        if (configuredVendor != null && !configuredVendor.isBlank() && !"auto".equalsIgnoreCase(configuredVendor)) {
            return configuredVendor.toLowerCase();
        }
        if (dataSource == null) {
            return null;
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName();
            if (productName == null) {
                return null;
            }
            String normalized = productName.toLowerCase();
            if (normalized.contains("postgres")) {
                return "postgresql";
            }
            if (normalized.contains("mysql") || normalized.contains("mariadb")) {
                return "mysql";
            }
            if (normalized.contains("h2")) {
                return "h2";
            }
            return null;
        }
        catch (Exception ex) {
            return null;
        }
    }

    private String schemaPrefix(String schema) {
        return schema == null || schema.isBlank() ? "" : schema + ".";
    }
}
