package io.orion.audit.autoconfigure.config;

import io.orion.audit.autoconfigure.driver.AuditLogEntityMapper;
import io.orion.audit.autoconfigure.driver.AuditLogRepository;
import io.orion.audit.autoconfigure.driver.CompositeAuditDriver;
import io.orion.audit.autoconfigure.driver.DatabaseAuditDriver;
import io.orion.audit.autoconfigure.driver.LoggingAuditDriver;
import io.orion.audit.autoconfigure.driver.NoopAuditDriver;
import io.orion.audit.autoconfigure.hibernate.HibernateAuditEventListener;
import io.orion.audit.autoconfigure.hibernate.HibernateAuditIntegrator;
import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.autoconfigure.service.AuditOrchestrator;
import io.orion.audit.autoconfigure.support.AuditListenerBridge;
import io.orion.audit.autoconfigure.support.AuditListenerModeResolver;
import io.orion.audit.autoconfigure.support.AuditRecordMapper;
import io.orion.audit.autoconfigure.query.DatabaseAuditQueryService;
import io.orion.audit.core.query.AuditQueryService;
import io.orion.audit.core.spi.AuditDriver;
import io.orion.audit.core.spi.ValueSerializer;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JPA-specific audit infrastructure.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(jakarta.persistence.EntityManager.class)
public class AuditJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AuditLogEntityMapper auditLogEntityMapper(ValueSerializer valueSerializer) {
        return new AuditLogEntityMapper(valueSerializer);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataSource.class)
    NamedParameterJdbcTemplate auditNamedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataSource.class)
    JdbcTemplate auditJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataSource.class)
    AuditSchemaInitializer auditSchemaInitializer(DataSource dataSource, JdbcTemplate auditJdbcTemplate, AuditProperties properties) {
        return new AuditSchemaInitializer(dataSource, auditJdbcTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "hibernateAuditEventListener")
    HibernateAuditEventListener hibernateAuditEventListener(AuditOrchestrator orchestrator, io.orion.audit.autoconfigure.support.AuditEntityIntrospector introspector) {
        return new HibernateAuditEventListener(orchestrator, introspector);
    }

    @Bean
    @ConditionalOnMissingBean(AuditDriver.class)
    AuditDriver auditDriver(
        AuditProperties properties,
        ObjectProvider<AuditLogRepository> auditLogRepositoryProvider,
        ObjectProvider<NamedParameterJdbcTemplate> jdbcTemplateProvider,
        ObjectProvider<AuditLogEntityMapper> mapperProvider,
        ValueSerializer valueSerializer
    ) {
        List<AuditDriver> delegates = new ArrayList<>();
        if (properties.isDatabaseDriverEnabled()) {
            NamedParameterJdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
            AuditLogEntityMapper mapper = mapperProvider.getIfAvailable();
            if (jdbcTemplate != null && mapper != null) {
                delegates.add(new DatabaseAuditDriver(auditLogRepositoryProvider.getIfAvailable(), mapper, jdbcTemplate, properties));
            }
        }
        if (properties.isLoggingDriverEnabled()) {
            delegates.add(new LoggingAuditDriver(valueSerializer));
        }
        if (delegates.isEmpty()) {
            return new NoopAuditDriver();
        }
        if (delegates.size() == 1) {
            return delegates.get(0);
        }
        return new CompositeAuditDriver(delegates);
    }

    @Bean
    @ConditionalOnBean(NamedParameterJdbcTemplate.class)
    @ConditionalOnMissingBean(AuditQueryService.class)
    AuditQueryService auditQueryService(
        ObjectProvider<AuditLogRepository> auditLogRepositoryProvider,
        NamedParameterJdbcTemplate jdbcTemplate,
        AuditProperties properties,
        AuditRecordMapper mapper
    ) {
        return new DatabaseAuditQueryService(auditLogRepositoryProvider.getIfAvailable(), jdbcTemplate, properties, mapper);
    }

    @Bean
    InitializingBean auditListenerBridgeInitializer(AuditOrchestrator orchestrator, AuditListenerModeResolver modeResolver) {
        return () -> {
            AuditListenerBridge.set(orchestrator);
            AuditListenerBridge.setModeResolver(modeResolver);
        };
    }

    @Bean
    @ConditionalOnMissingBean(name = "auditHibernatePropertiesCustomizer")
    HibernatePropertiesCustomizer auditHibernatePropertiesCustomizer(
        AuditProperties properties,
        AuditListenerModeResolver modeResolver,
        HibernateAuditEventListener hibernateAuditEventListener
    ) {
        return hibernateProperties -> {
            if (properties.getDdlAuto() != null && !properties.getDdlAuto().isBlank() && !"none".equalsIgnoreCase(properties.getDdlAuto())) {
                hibernateProperties.putIfAbsent("hibernate.hbm2ddl.auto", properties.getDdlAuto());
            }
            if (modeResolver.isHibernateListenerActive()) {
                IntegratorProvider existingProvider = existingIntegratorProvider(hibernateProperties.get("hibernate.integrator_provider"));
                hibernateProperties.put("hibernate.integrator_provider", (IntegratorProvider) () -> {
                    List<Integrator> integrators = new ArrayList<>();
                    if (existingProvider != null) {
                        integrators.addAll(existingProvider.getIntegrators());
                    }
                    integrators.add(new HibernateAuditIntegrator(hibernateAuditEventListener));
                    return integrators;
                });
            }
        };
    }

    private IntegratorProvider existingIntegratorProvider(Object candidate) {
        if (candidate instanceof IntegratorProvider integratorProvider) {
            return integratorProvider;
        }
        return null;
    }
}
