package io.orion.audit.autoconfigure.config;

import io.orion.audit.autoconfigure.properties.AuditProperties;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AuditFlywayAutoConfigurationTest {

    @Test
    void shouldAppendVendorSpecificAuditLocationToExistingLocations() {
        AuditProperties properties = new AuditProperties();
        AuditFlywayAutoConfiguration configuration = new AuditFlywayAutoConfiguration();
        FluentConfiguration flyway = new FluentConfiguration();
        flyway.locations("classpath:db/migration");

        FlywayConfigurationCustomizer customizer = configuration.auditFlywayConfigurationCustomizer(
            properties,
            new StaticListableBeanFactory(java.util.Map.of("dataSource", dataSource())).getBeanProvider(DataSource.class)
        );
        customizer.customize(flyway);

        assertThat(Arrays.stream(flyway.getLocations()).map(Object::toString).toList())
            .contains("classpath:db/migration", "classpath:META-INF/orion-audit/db/migration/h2");
        assertThat(flyway.getPlaceholders()).containsEntry("orion_audit_table", "audit_log");
    }

    @Test
    void shouldAllowExplicitVendorAndReplacementLocations() {
        AuditProperties properties = new AuditProperties();
        properties.getFlyway().setVendor("mysql");
        properties.getFlyway().setAppendToExisting(false);
        properties.getFlyway().getLocations().add("classpath:db/migration/common");
        AuditFlywayAutoConfiguration configuration = new AuditFlywayAutoConfiguration();
        FluentConfiguration flyway = new FluentConfiguration();
        flyway.locations("classpath:db/migration");

        FlywayConfigurationCustomizer customizer = configuration.auditFlywayConfigurationCustomizer(
            properties,
            new StaticListableBeanFactory().getBeanProvider(DataSource.class)
        );
        customizer.customize(flyway);

        assertThat(Arrays.stream(flyway.getLocations()).map(Object::toString).toList())
            .containsExactly("classpath:META-INF/orion-audit/db/migration/mysql", "classpath:db/migration/common");
    }

    private DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:flyway-test;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
