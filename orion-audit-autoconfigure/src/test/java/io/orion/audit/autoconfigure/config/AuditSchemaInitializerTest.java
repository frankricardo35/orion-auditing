package io.orion.audit.autoconfigure.config;

import io.orion.audit.autoconfigure.OrionAuditAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSchemaInitializerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, OrionAuditAutoConfiguration.class))
        .withPropertyValues(
            "spring.datasource.url=jdbc:h2:mem:audit-schema-init;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.flyway.enabled=false",
            "orion.audit.flyway.enabled=false"
        );

    @Test
    void shouldCreateAuditTableAutomaticallyWithoutFlyway() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            try (Connection connection = dataSource.getConnection();
                 ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), connection.getSchema(), "AUDIT_LOG", new String[]{"TABLE"})) {
                assertThat(tables.next()).isTrue();
            }
        });
    }
}
