package io.orion.audit.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Represents tenant metadata associated with the current audit operation.
 */
public final class TenantContext {

    private final String tenantId;
    private final Map<String, Object> attributes;

    public TenantContext(String tenantId, Map<String, Object> attributes) {
        this.tenantId = tenantId;
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static TenantContext of(String tenantId) {
        return new TenantContext(tenantId, Map.of());
    }

    public static TenantContext empty() {
        return new TenantContext(null, Map.of());
    }

    public String getTenantId() {
        return tenantId;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TenantContext that)) {
            return false;
        }
        return Objects.equals(tenantId, that.tenantId) && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, attributes);
    }
}
