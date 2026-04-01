package io.orion.audit.core.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEntryTest {

    @Test
    void shouldAllowNullValuesInSnapshotsAndChangeMaps() {
        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("lockTime", null);
        oldValues.put("status", null);

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("lockTime", null);
        newValues.put("status", "ACTIVE");

        Map<String, Object> changedField = new LinkedHashMap<>();
        changedField.put("old", null);
        changedField.put("new", "ACTIVE");

        AuditEntry entry = AuditEntry.builder()
            .action(AuditAction.UPDATE)
            .entityType("io.orion.User")
            .entityName("User")
            .entityId("7")
            .oldValues(oldValues)
            .newValues(newValues)
            .changedFields(Map.of("status", changedField))
            .tags(List.of("User", "update"))
            .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> statusChange = (Map<String, Object>) entry.getChangedFields().get("status");

        assertThat(entry.getOldValues()).containsEntry("lockTime", null);
        assertThat(entry.getNewValues()).containsEntry("status", "ACTIVE");
        assertThat(statusChange).containsEntry("old", null);
    }

    @Test
    void shouldCreateImmutableCopiesOfNestedStructures() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("old", null);
        nested.put("new", "alice@example.com");

        Map<String, Object> changedFields = new LinkedHashMap<>();
        changedFields.put("email", nested);

        AuditEntry entry = AuditEntry.builder()
            .action(AuditAction.UPDATE)
            .changedFields(changedFields)
            .build();

        nested.put("new", "mutated@example.com");
        changedFields.put("status", Map.of("old", "A", "new", "B"));

        assertThat(((Map<?, ?>) entry.getChangedFields().get("email")).get("new")).isEqualTo("alice@example.com");
        assertThat(entry.getChangedFields()).doesNotContainKey("status");
    }
}
