package io.orion.audit.autoconfigure.config;

import io.orion.audit.autoconfigure.OrionAuditAutoConfiguration;
import io.orion.audit.autoconfigure.properties.AuditDriverType;
import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.autoconfigure.properties.EntityTypeFormat;
import io.orion.audit.autoconfigure.support.AuditListenerModeResolver;
import io.orion.audit.core.model.AuditListenerMode;
import io.orion.audit.core.spi.ActorResolver;
import io.orion.audit.core.spi.EntityIdResolver;
import io.orion.audit.core.spi.RequestInfoResolver;
import io.orion.audit.core.spi.TenantResolver;
import io.orion.audit.core.spi.ValueSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AuditAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OrionAuditAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new);
    }

    @Test
    void shouldCreateDefaultAuditBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ValueSerializer.class);
            assertThat(context).hasSingleBean(EntityIdResolver.class);
            assertThat(context).hasSingleBean(ActorResolver.class);
            assertThat(context).hasSingleBean(RequestInfoResolver.class);
            assertThat(context).hasSingleBean(TenantResolver.class);
            assertThat(context).hasSingleBean(AuditListenerModeResolver.class);
            assertThat(context).hasSingleBean(io.orion.audit.autoconfigure.service.AuditOrchestrator.class);
        });
    }

    @Test
    void shouldBindV2Properties() {
        contextRunner
            .withPropertyValues(
                "orion.audit.listener-mode=jpa",
                "orion.audit.entity-type-format=simple",
                "orion.audit.drivers[0]=logging",
                "orion.audit.actors[0].type=users",
                "orion.audit.actors[0].principal-class=com.example.auth.UserPrincipal",
                "orion.audit.actors[0].id-property=id",
                "orion.audit.actors[0].name-property=fullName",
                "orion.audit.flyway.vendor=mysql",
                "orion.audit.flyway.append-to-existing=false",
                "orion.audit.flyway.locations[0]=classpath:db/migration/common"
            )
            .run(context -> {
                AuditProperties properties = context.getBean(AuditProperties.class);
                assertThat(properties.getListenerMode()).isEqualTo(AuditListenerMode.JPA);
                assertThat(properties.getEntityTypeFormat()).isEqualTo(EntityTypeFormat.SIMPLE);
                assertThat(properties.getDrivers()).containsExactly(AuditDriverType.LOGGING);
                assertThat(properties.isDatabaseDriverEnabled()).isFalse();
                assertThat(properties.isLoggingDriverEnabled()).isTrue();
                assertThat(properties.getActors()).hasSize(1);
                assertThat(properties.getActors().get(0).getType()).isEqualTo("users");
                assertThat(properties.getActors().get(0).getPrincipalClass()).isEqualTo("com.example.auth.UserPrincipal");
                assertThat(properties.getActors().get(0).getIdProperty()).isEqualTo("id");
                assertThat(properties.getActors().get(0).getNameProperty()).isEqualTo("fullName");
                assertThat(properties.getFlyway().getVendor()).isEqualTo("mysql");
                assertThat(properties.getFlyway().isAppendToExisting()).isFalse();
                assertThat(properties.getFlyway().getLocations()).containsExactly("classpath:db/migration/common");
            });
    }

    @Test
    void shouldExposeSensibleDefaults() {
        contextRunner.run(context -> {
            AuditProperties properties = context.getBean(AuditProperties.class);
            assertThat(properties.getListenerMode()).isEqualTo(AuditListenerMode.AUTO);
            assertThat(properties.getEntityTypeFormat()).isEqualTo(EntityTypeFormat.QUALIFIED);
            assertThat(properties.isDatabaseDriverEnabled()).isTrue();
            assertThat(properties.isLoggingDriverEnabled()).isFalse();
            assertThat(properties.getActors()).isEmpty();
            assertThat(properties.getFlyway().isEnabled()).isTrue();
            assertThat(properties.getFlyway().getVendor()).isEqualTo("auto");
            assertThat(properties.getFlyway().isAppendToExisting()).isTrue();
        });
    }
}
