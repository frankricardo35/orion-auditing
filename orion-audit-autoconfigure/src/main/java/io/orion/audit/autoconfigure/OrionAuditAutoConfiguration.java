package io.orion.audit.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.orion.audit.autoconfigure.config.AuditFlywayAutoConfiguration;
import io.orion.audit.autoconfigure.config.AuditJpaAutoConfiguration;
import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.autoconfigure.resolver.DefaultEntityIdResolver;
import io.orion.audit.autoconfigure.resolver.DefaultTenantResolver;
import io.orion.audit.autoconfigure.resolver.NoopActorResolver;
import io.orion.audit.autoconfigure.resolver.NoopRequestInfoResolver;
import io.orion.audit.autoconfigure.resolver.ServletRequestInfoResolver;
import io.orion.audit.autoconfigure.resolver.SpringSecurityActorResolver;
import io.orion.audit.autoconfigure.service.AuditOrchestrator;
import io.orion.audit.autoconfigure.support.AuditEntityIntrospector;
import io.orion.audit.autoconfigure.support.AuditJsonSupport;
import io.orion.audit.autoconfigure.support.AuditListenerModeResolver;
import io.orion.audit.autoconfigure.support.AuditRecordMapper;
import io.orion.audit.autoconfigure.support.AuditStateStore;
import io.orion.audit.autoconfigure.support.JacksonValueSerializer;
import io.orion.audit.core.spi.ActorResolver;
import io.orion.audit.core.spi.AuditFilter;
import io.orion.audit.core.spi.AuditModifier;
import io.orion.audit.core.spi.EntityIdResolver;
import io.orion.audit.core.spi.RequestInfoResolver;
import io.orion.audit.core.spi.TagResolver;
import io.orion.audit.core.spi.TenantResolver;
import io.orion.audit.core.spi.ValueSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 * Main auto-configuration entry point for orion-audit.
 */
@AutoConfiguration
@org.springframework.boot.autoconfigure.AutoConfigureAfter(name = {
    "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
    "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@AutoConfigurationPackage
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnBooleanProperty(prefix = "orion.audit", name = "enabled", matchIfMissing = true)
@Import({AuditJpaAutoConfiguration.class, AuditFlywayAutoConfiguration.class})
public class OrionAuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ValueSerializer auditValueSerializer(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(() -> JsonMapper.builder().findAndAddModules().build());
        return new JacksonValueSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    AuditJsonSupport auditJsonSupport(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new AuditJsonSupport(objectMapperProvider.getIfAvailable(() -> JsonMapper.builder().findAndAddModules().build()));
    }

    @Bean
    @ConditionalOnMissingBean
    AuditRecordMapper auditRecordMapper(AuditJsonSupport auditJsonSupport) {
        return new AuditRecordMapper(auditJsonSupport);
    }

    @Bean
    @ConditionalOnMissingBean
    EntityIdResolver entityIdResolver() {
        return new DefaultEntityIdResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    ActorResolver actorResolver(AuditProperties properties) {
        if (properties.isUseSpringSecurity()
            && ClassUtils.isPresent("org.springframework.security.core.context.SecurityContextHolder", getClass().getClassLoader())) {
            return new SpringSecurityActorResolver(properties.getActors());
        }
        return new NoopActorResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    RequestInfoResolver requestInfoResolver(AuditProperties properties) {
        if (properties.isCaptureRequestInfo()
            && ClassUtils.isPresent("jakarta.servlet.http.HttpServletRequest", getClass().getClassLoader())) {
            return new ServletRequestInfoResolver(properties.getDefaultSource());
        }
        return new NoopRequestInfoResolver(properties.getDefaultSource());
    }

    @Bean
    @ConditionalOnMissingBean
    TenantResolver tenantResolver() {
        return new DefaultTenantResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    AuditEntityIntrospector auditEntityIntrospector(AuditProperties properties) {
        return new AuditEntityIntrospector(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    AuditStateStore auditStateStore() {
        return new AuditStateStore();
    }

    @Bean
    @ConditionalOnMissingBean
    AuditListenerModeResolver auditListenerModeResolver(AuditProperties properties) {
        return new AuditListenerModeResolver(properties, getClass().getClassLoader());
    }

    @Bean
    @ConditionalOnMissingBean
    AuditOrchestrator auditOrchestrator(
        AuditProperties properties,
        AuditEntityIntrospector introspector,
        EntityIdResolver entityIdResolver,
        ActorResolver actorResolver,
        RequestInfoResolver requestInfoResolver,
        io.orion.audit.core.spi.AuditDriver auditDriver,
        AuditStateStore auditStateStore,
        AuditListenerModeResolver listenerModeResolver,
        ObjectProvider<List<AuditFilter>> filtersProvider,
        ObjectProvider<List<TagResolver>> tagResolversProvider,
        ObjectProvider<List<AuditModifier>> modifiersProvider,
        ObjectProvider<List<TenantResolver>> tenantResolversProvider
    ) {
        return new AuditOrchestrator(
            properties,
            introspector,
            entityIdResolver,
            actorResolver,
            requestInfoResolver,
            auditDriver,
            auditStateStore,
            listenerModeResolver,
            filtersProvider.getIfAvailable(List::of),
            tagResolversProvider.getIfAvailable(List::of),
            modifiersProvider.getIfAvailable(List::of),
            tenantResolversProvider.getIfAvailable(List::of)
        );
    }
}
