package io.orion.audit.autoconfigure.service;

import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.autoconfigure.support.AuditDiffResult;
import io.orion.audit.autoconfigure.support.AuditDiffSupport;
import io.orion.audit.autoconfigure.support.AuditEntityDescriptor;
import io.orion.audit.autoconfigure.support.AuditEntityIntrospector;
import io.orion.audit.autoconfigure.support.AuditListenerModeResolver;
import io.orion.audit.autoconfigure.support.AuditStateStore;
import io.orion.audit.autoconfigure.support.AuditTagSupport;
import io.orion.audit.core.model.ActorInfo;
import io.orion.audit.core.model.AuditAction;
import io.orion.audit.core.model.AuditEntry;
import io.orion.audit.core.model.RequestInfo;
import io.orion.audit.core.model.TenantContext;
import io.orion.audit.core.spi.ActorResolver;
import io.orion.audit.core.spi.AuditDriver;
import io.orion.audit.core.spi.AuditFilter;
import io.orion.audit.core.spi.AuditModifier;
import io.orion.audit.core.spi.EntityIdResolver;
import io.orion.audit.core.spi.RequestInfoResolver;
import io.orion.audit.core.spi.TagResolver;
import io.orion.audit.core.spi.TenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Central auditing service shared by JPA and Hibernate listener integrations.
 */
public class AuditOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AuditOrchestrator.class);

    private final AuditProperties properties;
    private final AuditEntityIntrospector introspector;
    private final EntityIdResolver entityIdResolver;
    private final ActorResolver actorResolver;
    private final RequestInfoResolver requestInfoResolver;
    private final AuditDriver auditDriver;
    private final AuditStateStore auditStateStore;
    private final AuditListenerModeResolver listenerModeResolver;
    private final List<AuditFilter> filters;
    private final List<TagResolver> tagResolvers;
    private final List<AuditModifier> auditModifiers;
    private final List<TenantResolver> tenantResolvers;

    public AuditOrchestrator(
        AuditProperties properties,
        AuditEntityIntrospector introspector,
        EntityIdResolver entityIdResolver,
        ActorResolver actorResolver,
        RequestInfoResolver requestInfoResolver,
        AuditDriver auditDriver,
        AuditStateStore auditStateStore,
        AuditListenerModeResolver listenerModeResolver,
        List<AuditFilter> filters,
        List<TagResolver> tagResolvers,
        List<AuditModifier> auditModifiers,
        List<TenantResolver> tenantResolvers
    ) {
        this.properties = properties;
        this.introspector = introspector;
        this.entityIdResolver = entityIdResolver;
        this.actorResolver = actorResolver;
        this.requestInfoResolver = requestInfoResolver;
        this.auditDriver = auditDriver;
        this.auditStateStore = auditStateStore;
        this.listenerModeResolver = listenerModeResolver;
        this.filters = ordered(filters);
        this.tagResolvers = ordered(tagResolvers);
        this.auditModifiers = ordered(auditModifiers);
        this.tenantResolvers = ordered(tenantResolvers);
    }

    public void onPostLoad(Object entity) {
        if (!listenerModeResolver.isJpaListenerActive()) {
            return;
        }
        introspector.getDescriptor(entity.getClass())
            .filter(descriptor -> descriptor.supports(AuditAction.UPDATE) || descriptor.supports(AuditAction.DELETE))
            .ifPresent(descriptor -> auditStateStore.putSnapshot(entity, introspector.extractValues(entity, descriptor)));
    }

    public void onPostPersist(Object entity) {
        if (!listenerModeResolver.isJpaListenerActive()) {
            return;
        }
        descriptor(entity, AuditAction.INSERT).ifPresent(descriptor -> {
            if (!auditStateStore.markInserted(entity)) {
                return;
            }
            Map<String, Object> current = introspector.extractValues(entity, descriptor);
            recordInsert(entity, descriptor, AuditDiffSupport.diff(Map.of(), current, true));
        });
    }

    public void onPreUpdate(Object entity) {
        if (!listenerModeResolver.isJpaListenerActive()) {
            return;
        }
        descriptor(entity, AuditAction.UPDATE).ifPresent(descriptor -> {
            Map<String, Object> before = defaultedSnapshot(entity, descriptor);
            Map<String, Object> after = introspector.extractValues(entity, descriptor);
            AuditDiffResult diff = AuditDiffSupport.diff(before, after, descriptor.storeFullSnapshot());
            if (!shouldPublish(diff, AuditAction.UPDATE)) {
                auditStateStore.removeSnapshot(entity);
                return;
            }
            AuditEntry entry = applyEnrichers(buildEntry(entity, descriptor, AuditAction.UPDATE, diff));
            auditStateStore.putPending(entity, () -> publish(entry));
            auditStateStore.putSnapshot(entity, after);
        });
    }

    public void onPostUpdate(Object entity) {
        if (!listenerModeResolver.isJpaListenerActive()) {
            return;
        }
        Runnable pending = auditStateStore.removePending(entity);
        if (pending != null) {
            pending.run();
        }
    }

    public void onPreRemove(Object entity) {
        if (!listenerModeResolver.isJpaListenerActive()) {
            return;
        }
        descriptor(entity, AuditAction.DELETE).ifPresent(descriptor -> {
            Map<String, Object> before = defaultedSnapshot(entity, descriptor);
            AuditDiffResult diff = AuditDiffSupport.diff(before, Map.of(), true);
            AuditEntry entry = applyEnrichers(buildEntry(entity, descriptor, AuditAction.DELETE, diff));
            auditStateStore.putPending(entity, () -> publish(entry));
            auditStateStore.removeSnapshot(entity);
        });
    }

    public void onPostRemove(Object entity) {
        if (!listenerModeResolver.isJpaListenerActive()) {
            return;
        }
        Runnable pending = auditStateStore.removePending(entity);
        if (pending != null) {
            pending.run();
        }
    }

    public void recordInsert(Object entity, AuditEntityDescriptor descriptor, AuditDiffResult diff) {
        if (!shouldPublish(diff, AuditAction.INSERT)) {
            return;
        }
        publish(applyEnrichers(buildEntry(entity, descriptor, AuditAction.INSERT, diff)));
    }

    public void recordUpdate(Object entity, AuditEntityDescriptor descriptor, AuditDiffResult diff) {
        if (!shouldPublish(diff, AuditAction.UPDATE)) {
            return;
        }
        publish(applyEnrichers(buildEntry(entity, descriptor, AuditAction.UPDATE, diff)));
    }

    public void recordDelete(Object entity, AuditEntityDescriptor descriptor, AuditDiffResult diff) {
        if (!shouldPublish(diff, AuditAction.DELETE)) {
            return;
        }
        publish(applyEnrichers(buildEntry(entity, descriptor, AuditAction.DELETE, diff)));
    }

    private Map<String, Object> defaultedSnapshot(Object entity, AuditEntityDescriptor descriptor) {
        Map<String, Object> snapshot = auditStateStore.removeSnapshot(entity);
        return snapshot != null ? snapshot : introspector.extractValues(entity, descriptor);
    }

    private Optional<AuditEntityDescriptor> descriptor(Object entity, AuditAction action) {
        if (entity == null || !properties.isEnabled() || !properties.getActions().contains(action)) {
            return Optional.empty();
        }
        Optional<AuditEntityDescriptor> descriptor = introspector.getDescriptor(entity.getClass()).filter(item -> item.supports(action));
        if (descriptor.isEmpty()) {
            return Optional.empty();
        }
        for (AuditFilter filter : filters) {
            if (!filter.shouldAudit(entity.getClass(), action)) {
                return Optional.empty();
            }
        }
        return descriptor;
    }

    private AuditEntry buildEntry(
        Object entity,
        AuditEntityDescriptor descriptor,
        AuditAction action,
        AuditDiffResult diff
    ) {
        ActorInfo actor = actorResolver.resolve();
        RequestInfo request = requestInfoResolver.resolve();
        TenantContext tenantContext = resolveTenantContext();
        String tenantId = tenantContext != null && tenantContext.getTenantId() != null ? tenantContext.getTenantId() : actor.getTenantId();
        return AuditEntry.builder()
            .id(UUID.randomUUID().toString())
            .action(action)
            .entityType(descriptor.entityType())
            .entityName(descriptor.entityName())
            .entityId(entityIdResolver.resolveEntityId(entity))
            .oldValues(diff.oldValues())
            .newValues(diff.newValues())
            .changedFields(diff.changedFields())
            .actorId(actor.getActorId())
            .actorName(actor.getActorName())
            .actorType(actor.getActorType())
            .ipAddress(request.getIpAddress())
            .userAgent(request.getUserAgent())
            .requestUri(request.getRequestUri())
            .httpMethod(request.getHttpMethod())
            .traceId(request.getTraceId())
            .source(request.getSource() != null ? request.getSource() : properties.getDefaultSource())
            .tenantId(tenantId)
            .tags(AuditTagSupport.normalize(List.of(descriptor.entityName(), action.name().toLowerCase())))
            .createdAt(Instant.now())
            .build();
    }

    private AuditEntry applyEnrichers(AuditEntry entry) {
        LinkedHashSet<String> tags = new LinkedHashSet<>(entry.getTags());
        for (TagResolver tagResolver : tagResolvers) {
            tags.addAll(AuditTagSupport.normalize(tagResolver.resolveTags(entry)));
        }
        AuditEntry current = entry.toBuilder().tags(AuditTagSupport.normalize(tags)).build();
        for (AuditModifier auditModifier : auditModifiers) {
            AuditEntry modified = auditModifier.modify(current);
            if (modified == null) {
                throw new IllegalStateException("AuditModifier returned null for entry " + current.getId());
            }
            current = modified.toBuilder().tags(AuditTagSupport.normalize(modified.getTags())).build();
        }
        return current;
    }

    private TenantContext resolveTenantContext() {
        for (TenantResolver tenantResolver : tenantResolvers) {
            TenantContext tenantContext = tenantResolver.resolveTenant();
            if (tenantContext != null) {
                return tenantContext;
            }
        }
        return null;
    }

    private boolean shouldPublish(AuditDiffResult diff, AuditAction action) {
        return action != AuditAction.UPDATE || properties.isStoreEmptyChanges() || !diff.changedFields().isEmpty();
    }

    private void publish(AuditEntry entry) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    persistEntry(entry);
                }
            });
        }
        else {
            persistEntry(entry);
        }
    }

    private void persistEntry(AuditEntry entry) {
        try {
            auditDriver.save(entry);
        }
        catch (RuntimeException ex) {
            if (properties.isFailOnError()) {
                throw ex;
            }
            log.warn("Failed to persist audit entry for {} {}", entry.getEntityType(), entry.getEntityId(), ex);
        }
    }

    private <T> List<T> ordered(List<T> beans) {
        if (beans == null || beans.isEmpty()) {
            return List.of();
        }
        ArrayList<T> ordered = new ArrayList<>(beans);
        AnnotationAwareOrderComparator.sort(ordered);
        return List.copyOf(ordered);
    }
}
