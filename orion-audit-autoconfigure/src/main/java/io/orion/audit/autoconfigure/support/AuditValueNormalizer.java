package io.orion.audit.autoconfigure.support;

import org.springframework.beans.BeanUtils;

import java.lang.reflect.Array;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Normalizes reflected entity values into audit-friendly payload values.
 */
public final class AuditValueNormalizer {

    private AuditValueNormalizer() {
    }

    public static Object normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (BeanUtils.isSimpleValueType(value.getClass()) || value instanceof Temporal || value instanceof UUID) {
            return value;
        }
        if (value.getClass().isEnum()) {
            return ((Enum<?>) value).name();
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            Collection<Object> normalized = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                normalized.add(normalize(Array.get(value, index)));
            }
            return normalized;
        }
        if (value instanceof Collection<?> collection) {
            Collection<Object> normalized = new ArrayList<>(collection.size());
            for (Object element : collection) {
                normalized.add(normalize(element));
            }
            return normalized;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
            }
            return normalized;
        }
        return String.valueOf(value);
    }
}
