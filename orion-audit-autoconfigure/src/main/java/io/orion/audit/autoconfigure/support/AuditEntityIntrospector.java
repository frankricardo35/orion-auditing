package io.orion.audit.autoconfigure.support;

import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.autoconfigure.properties.EntityTypeFormat;
import io.orion.audit.core.annotation.Audited;
import io.orion.audit.core.util.AuditReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches auditable entity metadata and extracts field snapshots.
 */
public class AuditEntityIntrospector {

    private final AuditProperties properties;
    private final Map<Class<?>, Optional<AuditEntityDescriptor>> cache = new ConcurrentHashMap<>();

    public AuditEntityIntrospector(AuditProperties properties) {
        this.properties = properties;
    }

    public Optional<AuditEntityDescriptor> getDescriptor(Class<?> entityClass) {
        return cache.computeIfAbsent(entityClass, this::createDescriptor);
    }

    public Map<String, Object> extractValues(Object entity, AuditEntityDescriptor descriptor) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (AuditFieldDescriptor fieldDescriptor : descriptor.fields()) {
            Field field = fieldDescriptor.field();
            AuditReflectionUtils.readField(field, entity)
                .ifPresent(value -> values.put(fieldDescriptor.auditName(), AuditValueNormalizer.normalize(value)));
        }
        return values;
    }

    public static void putIfAuditable(
        AuditEntityDescriptor descriptor,
        Map<String, Object> target,
        String sourceFieldName,
        Object rawValue
    ) {
        AuditFieldDescriptor fieldDescriptor = descriptor.fieldBySourceName(sourceFieldName);
        if (fieldDescriptor != null) {
            target.put(fieldDescriptor.auditName(), AuditValueNormalizer.normalize(rawValue));
        }
    }

    private Optional<AuditEntityDescriptor> createDescriptor(Class<?> entityClass) {
        Audited audited = entityClass.getAnnotation(Audited.class);
        if (audited == null) {
            return Optional.empty();
        }

        Set<String> included = Set.of(audited.include());
        Set<String> ignored = new java.util.LinkedHashSet<>(properties.getIgnoredFields());
        ignored.addAll(Arrays.asList(audited.ignore()));
        List<AuditFieldDescriptor> descriptors = new ArrayList<>();
        for (Field field : AuditReflectionUtils.getAllFields(entityClass)) {
            String sourceName = field.getName();
            if (!included.isEmpty() && !included.contains(sourceName)) {
                continue;
            }
            if (ignored.contains(sourceName) || AuditReflectionUtils.isIgnored(field)) {
                continue;
            }
            descriptors.add(new AuditFieldDescriptor(field, sourceName, AuditReflectionUtils.resolveAuditFieldName(field)));
        }

        String entityName = audited.label().isBlank() ? entityClass.getSimpleName() : audited.label();
        Set<io.orion.audit.core.model.AuditAction> actions = audited.actions().length == 0
            ? EnumSet.noneOf(io.orion.audit.core.model.AuditAction.class)
            : EnumSet.copyOf(List.of(audited.actions()));

        return Optional.of(new AuditEntityDescriptor(
            entityClass,
            properties.getEntityTypeFormat() == EntityTypeFormat.SIMPLE ? entityClass.getSimpleName() : entityClass.getName(),
            entityName,
            actions,
            audited.storeFullSnapshot() || properties.isStoreFullSnapshot(),
            descriptors
        ));
    }
}
