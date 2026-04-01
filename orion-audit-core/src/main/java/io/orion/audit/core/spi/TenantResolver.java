package io.orion.audit.core.spi;

import io.orion.audit.core.model.TenantContext;

/**
 * Resolves the current tenant context when multi-tenant information is available.
 */
public interface TenantResolver {

    /**
     * Resolves tenant information for the current execution context.
     *
     * @return tenant context or {@code null} when unavailable
     */
    TenantContext resolveTenant();
}
