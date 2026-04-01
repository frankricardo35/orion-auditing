package io.orion.audit.core.util;

import io.orion.audit.core.annotation.AuditField;
import io.orion.audit.core.annotation.AuditIgnore;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reflection helper methods shared by the auditing infrastructure.
 */
public final class AuditReflectionUtils {

    private AuditReflectionUtils() {
    }

    /**
     * Returns all declared instance fields for a class hierarchy.
     *
     * @param type type to inspect
     * @return fields in top-down order
     */
    public static List<Field> getAllFields(Class<?> type) {
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            hierarchy.add(0, current);
            current = current.getSuperclass();
        }

        List<Field> fields = new ArrayList<>();
        for (Class<?> candidate : hierarchy) {
            for (Field field : candidate.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                fields.add(field);
            }
        }
        return fields;
    }

    /**
     * Reads a field value without failing on inaccessible members.
     *
     * @param field field to read
     * @param target source object
     * @return field value if readable
     */
    public static Optional<Object> readField(Field field, Object target) {
        boolean accessible = field.canAccess(target);
        try {
            if (!accessible) {
                field.setAccessible(true);
            }
            return Optional.ofNullable(field.get(target));
        }
        catch (RuntimeException ex) {
            return Optional.empty();
        }
        catch (IllegalAccessException ex) {
            return Optional.empty();
        }
        finally {
            if (!accessible) {
                try {
                    field.setAccessible(false);
                }
                catch (RuntimeException ignored) {
                }
            }
        }
    }

    /**
     * @param field field to inspect
     * @return audit field name, taking {@link AuditField} into account
     */
    public static String resolveAuditFieldName(Field field) {
        AuditField auditField = field.getAnnotation(AuditField.class);
        return auditField != null && !auditField.value().isBlank() ? auditField.value() : field.getName();
    }

    /**
     * @param field field to inspect
     * @return whether the field is explicitly ignored
     */
    public static boolean isIgnored(Field field) {
        return field.isAnnotationPresent(AuditIgnore.class);
    }

    /**
     * Extracts all readable field values using audit field names.
     *
     * @param source entity instance
     * @return audit field values
     */
    public static Map<String, Object> extractReadableValues(Object source) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Field field : getAllFields(source.getClass())) {
            readField(field, source).ifPresent(value -> values.put(resolveAuditFieldName(field), value));
        }
        return values;
    }
}
