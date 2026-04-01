package io.orion.audit.autoconfigure.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builds old/new/changed payloads for audit entries.
 */
public final class AuditDiffSupport {

    private AuditDiffSupport() {
    }

    public static AuditDiffResult diff(Map<String, Object> before, Map<String, Object> after, boolean fullSnapshot) {
        Map<String, Object> changed = new LinkedHashMap<>();
        Map<String, Object> oldValues = fullSnapshot ? new LinkedHashMap<>(before) : new LinkedHashMap<>();
        Map<String, Object> newValues = fullSnapshot ? new LinkedHashMap<>(after) : new LinkedHashMap<>();

        for (String key : unionKeys(before, after).keySet()) {
            Object oldValue = before.get(key);
            Object newValue = after.get(key);
            if (!Objects.equals(oldValue, newValue)) {
                Map<String, Object> delta = new LinkedHashMap<>();
                delta.put("old", oldValue);
                delta.put("new", newValue);
                changed.put(key, delta);
                if (!fullSnapshot) {
                    oldValues.put(key, oldValue);
                    newValues.put(key, newValue);
                }
            }
        }
        return new AuditDiffResult(oldValues, newValues, changed);
    }

    private static Map<String, Boolean> unionKeys(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Boolean> keys = new LinkedHashMap<>();
        before.keySet().forEach(key -> keys.put(key, Boolean.TRUE));
        after.keySet().forEach(key -> keys.put(key, Boolean.TRUE));
        return keys;
    }
}
