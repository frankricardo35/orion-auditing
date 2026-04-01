package io.orion.audit.autoconfigure.hibernate;

import io.orion.audit.autoconfigure.service.AuditOrchestrator;
import io.orion.audit.autoconfigure.support.AuditDiffResult;
import io.orion.audit.autoconfigure.support.AuditEntityDescriptor;
import io.orion.audit.autoconfigure.support.AuditEntityIntrospector;
import io.orion.audit.core.model.AuditAction;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Hibernate-native listener that uses ORM state arrays for accurate audit diffs.
 */
public class HibernateAuditEventListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private final AuditOrchestrator orchestrator;
    private final AuditEntityIntrospector introspector;

    public HibernateAuditEventListener(AuditOrchestrator orchestrator, AuditEntityIntrospector introspector) {
        this.orchestrator = orchestrator;
        this.introspector = introspector;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        descriptor(event.getEntity(), AuditAction.INSERT).ifPresent(descriptor -> {
            Map<String, Object> newValues = extractState(descriptor, event.getPersister(), event.getState());
            orchestrator.recordInsert(event.getEntity(), descriptor, new AuditDiffResult(Map.of(), newValues, changeMap(Map.of(), newValues)));
        });
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        descriptor(event.getEntity(), AuditAction.UPDATE).ifPresent(descriptor -> {
            Map<String, Object> before = extractDirtyState(descriptor, event.getPersister(), event.getOldState(), event.getDirtyProperties());
            Map<String, Object> after = extractDirtyState(descriptor, event.getPersister(), event.getState(), event.getDirtyProperties());
            orchestrator.recordUpdate(event.getEntity(), descriptor, new AuditDiffResult(before, after, changeMap(before, after)));
        });
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        descriptor(event.getEntity(), AuditAction.DELETE).ifPresent(descriptor -> {
            Map<String, Object> before = extractState(descriptor, event.getPersister(), event.getDeletedState());
            orchestrator.recordDelete(event.getEntity(), descriptor, new AuditDiffResult(before, Map.of(), changeMap(before, Map.of())));
        });
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private Optional<AuditEntityDescriptor> descriptor(Object entity, AuditAction action) {
        return introspector.getDescriptor(entity.getClass()).filter(descriptor -> descriptor.supports(action));
    }

    private Map<String, Object> extractState(AuditEntityDescriptor descriptor, EntityPersister persister, Object[] state) {
        if (state == null) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        String[] propertyNames = persister.getPropertyNames();
        for (int index = 0; index < propertyNames.length; index++) {
            AuditEntityIntrospector.putIfAuditable(descriptor, values, propertyNames[index], state[index]);
        }
        return values;
    }

    private Map<String, Object> extractDirtyState(AuditEntityDescriptor descriptor, EntityPersister persister, Object[] state, int[] dirtyProperties) {
        if (state == null || dirtyProperties == null || dirtyProperties.length == 0) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        String[] propertyNames = persister.getPropertyNames();
        for (int dirtyProperty : dirtyProperties) {
            if (dirtyProperty >= 0 && dirtyProperty < propertyNames.length) {
                AuditEntityIntrospector.putIfAuditable(descriptor, values, propertyNames[dirtyProperty], state[dirtyProperty]);
            }
        }
        return values;
    }

    private Map<String, Object> changeMap(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> changed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : before.entrySet()) {
            Map<String, Object> delta = new LinkedHashMap<>();
            delta.put("old", entry.getValue());
            delta.put("new", after.get(entry.getKey()));
            changed.put(entry.getKey(), delta);
        }
        for (Map.Entry<String, Object> entry : after.entrySet()) {
            changed.computeIfAbsent(entry.getKey(), key -> {
                Map<String, Object> delta = new LinkedHashMap<>();
                delta.put("old", before.get(key));
                delta.put("new", entry.getValue());
                return delta;
            });
        }
        return changed;
    }
}
