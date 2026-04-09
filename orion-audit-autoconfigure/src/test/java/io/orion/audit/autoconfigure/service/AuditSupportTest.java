package io.orion.audit.autoconfigure.service;

import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.autoconfigure.properties.EntityTypeFormat;
import io.orion.audit.autoconfigure.resolver.DefaultEntityIdResolver;
import io.orion.audit.autoconfigure.resolver.NoopActorResolver;
import io.orion.audit.autoconfigure.resolver.NoopRequestInfoResolver;
import io.orion.audit.autoconfigure.support.AuditDiffResult;
import io.orion.audit.autoconfigure.support.AuditDiffSupport;
import io.orion.audit.autoconfigure.support.AuditEntityDescriptor;
import io.orion.audit.autoconfigure.support.AuditEntityIntrospector;
import io.orion.audit.autoconfigure.support.AuditListenerModeResolver;
import io.orion.audit.autoconfigure.support.AuditStateStore;
import io.orion.audit.core.annotation.AuditIgnore;
import io.orion.audit.core.annotation.Audited;
import io.orion.audit.core.model.AuditAction;
import io.orion.audit.core.model.AuditEntry;
import io.orion.audit.core.model.TenantContext;
import io.orion.audit.core.spi.AuditModifier;
import io.orion.audit.core.spi.TagResolver;
import io.orion.audit.core.spi.TenantResolver;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSupportTest {

    @Test
    void shouldRespectIncludeAndIgnoreRules() {
        AuditProperties properties = new AuditProperties();
        properties.getIgnoredFields().add("email");
        AuditEntityIntrospector introspector = new AuditEntityIntrospector(properties);

        AuditEntityDescriptor descriptor = introspector.getDescriptor(SampleEntity.class).orElseThrow();
        Map<String, Object> values = introspector.extractValues(new SampleEntity(10L, "Ada", "ada@example.com", "secret"), descriptor);

        assertThat(values).containsEntry("displayName", "Ada");
        assertThat(values).doesNotContainKeys("email", "password");
    }

    @Test
    void shouldResolveEntityId() {
        DefaultEntityIdResolver resolver = new DefaultEntityIdResolver();

        assertThat(resolver.resolveEntityId(new SampleEntity(55L, "Ada", "ada@example.com", "secret"))).isEqualTo("55");
    }

    @Test
    void shouldComputeChangedFields() {
        var result = AuditDiffSupport.diff(Map.of("name", "before"), Map.of("name", "after"), false);

        assertThat(result.oldValues()).containsEntry("name", "before");
        assertThat(result.newValues()).containsEntry("name", "after");
        assertThat(result.changedFields()).containsKey("name");
    }

    @Test
    void shouldUseSimpleEntityTypeWhenConfigured() {
        AuditProperties properties = new AuditProperties();
        properties.setEntityTypeFormat(EntityTypeFormat.SIMPLE);
        AuditEntityIntrospector introspector = new AuditEntityIntrospector(properties);

        AuditEntityDescriptor descriptor = introspector.getDescriptor(SampleEntity.class).orElseThrow();

        assertThat(descriptor.entityType()).isEqualTo("SampleEntity");
    }

    @Test
    void shouldApplyOrderedTagsModifiersAndTenantResolution() {
        AuditProperties properties = new AuditProperties();
        properties.setListenerMode(io.orion.audit.core.model.AuditListenerMode.JPA);
        AuditEntityIntrospector introspector = new AuditEntityIntrospector(properties);
        AuditEntityDescriptor descriptor = introspector.getDescriptor(SampleEntity.class).orElseThrow();
        List<AuditEntry> entries = new ArrayList<>();

        AuditOrchestrator orchestrator = new AuditOrchestrator(
            properties,
            introspector,
            new DefaultEntityIdResolver(),
            new NoopActorResolver(),
            new NoopRequestInfoResolver("tests"),
            entries::add,
            new AuditStateStore(),
            new AuditListenerModeResolver(properties, getClass().getClassLoader()),
            List.of(),
            List.of(new OrderedTagResolver(2, "module:billing"), new OrderedTagResolver(1, "role:admin"), entry -> List.of("")),
            List.of(
                new SourceModifier(1, "scheduler"),
                new TagAppendingModifier(2, "modifier:applied")
            ),
            List.of(() -> TenantContext.of("tenant-42"))
        );

        SampleEntity entity = new SampleEntity(1L, "Ada", "ada@example.com", "secret");
        AuditDiffResult diff = AuditDiffSupport.diff(Map.of(), introspector.extractValues(entity, descriptor), true);

        orchestrator.recordInsert(entity, descriptor, diff);

        assertThat(entries).hasSize(1);
        AuditEntry entry = entries.get(0);
        assertThat(entry.getTenantId()).isEqualTo("tenant-42");
        assertThat(entry.getSource()).isEqualTo("scheduler");
        assertThat(entry.getTags()).containsExactly("SampleEntity", "insert", "role:admin", "module:billing", "modifier:applied");
    }

    @Test
    void shouldAuditUpdateThroughJpaFallbackCallbacks() {
        AuditProperties properties = new AuditProperties();
        properties.setListenerMode(io.orion.audit.core.model.AuditListenerMode.JPA);
        AuditEntityIntrospector introspector = new AuditEntityIntrospector(properties);
        List<AuditEntry> entries = new ArrayList<>();
        AuditOrchestrator orchestrator = new AuditOrchestrator(
            properties,
            introspector,
            new DefaultEntityIdResolver(),
            new NoopActorResolver(),
            new NoopRequestInfoResolver("tests"),
            entries::add,
            new AuditStateStore(),
            new AuditListenerModeResolver(properties, getClass().getClassLoader()),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        SampleEntity entity = new SampleEntity(11L, "Before", "before@example.com", "secret");
        orchestrator.onPostLoad(entity);
        entity.setName("After");
        orchestrator.onPreUpdate(entity);
        orchestrator.onPostUpdate(entity);

        assertThat(entries).hasSize(1);
        AuditEntry entry = entries.get(0);
        assertThat(entry.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(entry.getChangedFields()).containsKey("displayName");
        assertThat(entry.getChangedFields()).doesNotContainKey("password");
    }

    @Audited(include = {"id", "name", "password"}, ignore = {"password"})
    private static class SampleEntity {
        @jakarta.persistence.Id
        private Long id;
        @io.orion.audit.core.annotation.AuditField("displayName")
        private String name;
        private String email;
        @AuditIgnore
        private String password;

        private SampleEntity(Long id, String name, String email, String password) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.password = password;
        }

        private void setName(String name) {
            this.name = name;
        }
    }

    private record OrderedTagResolver(int order, String tag) implements TagResolver, Ordered {
        @Override
        public java.util.Collection<String> resolveTags(AuditEntry entry) {
            return List.of(tag);
        }

        @Override
        public int getOrder() {
            return order;
        }
    }

    private record SourceModifier(int order, String source) implements AuditModifier, Ordered {
        @Override
        public AuditEntry modify(AuditEntry entry) {
            return entry.toBuilder().source(source).build();
        }

        @Override
        public int getOrder() {
            return order;
        }
    }

    private record TagAppendingModifier(int order, String tag) implements AuditModifier, Ordered {
        @Override
        public AuditEntry modify(AuditEntry entry) {
            List<String> tags = new ArrayList<>(entry.getTags());
            tags.add(tag);
            return entry.toBuilder().tags(tags).build();
        }

        @Override
        public int getOrder() {
            return order;
        }
    }
}
