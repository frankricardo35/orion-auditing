package io.orion.audit.autoconfigure.resolver;

import io.orion.audit.core.spi.EntityIdResolver;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import java.lang.reflect.Field;

/**
 * Reflective JPA identifier resolver.
 */
public class DefaultEntityIdResolver implements EntityIdResolver {

    @Override
    public String resolveEntityId(Object entity) {
        if (entity == null) {
            return null;
        }
        Class<?> type = entity.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                    boolean accessible = field.canAccess(entity);
                    try {
                        if (!accessible) {
                            field.setAccessible(true);
                        }
                        Object value = field.get(entity);
                        return value == null ? null : String.valueOf(value);
                    }
                    catch (IllegalAccessException ex) {
                        return null;
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
            }
            type = type.getSuperclass();
        }
        return null;
    }
}
