package io.orion.audit.autoconfigure.support;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Helpers for normalizing audit tags.
 */
public final class AuditTagSupport {

    private AuditTagSupport() {
    }

    public static List<String> normalize(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag != null) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }
        return List.copyOf(normalized);
    }
}
