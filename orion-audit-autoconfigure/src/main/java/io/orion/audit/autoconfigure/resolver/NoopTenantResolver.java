package io.orion.audit.autoconfigure.resolver;

import io.orion.audit.core.model.TenantContext;
import io.orion.audit.core.spi.TenantResolver;

/**
 * Default tenant resolver that provides no tenant context.
 */
public class NoopTenantResolver implements TenantResolver {

    @Override
    public TenantContext resolveTenant() {
        return null;
    }
}
