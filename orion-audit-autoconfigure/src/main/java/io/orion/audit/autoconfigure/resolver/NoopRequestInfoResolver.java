package io.orion.audit.autoconfigure.resolver;

import io.orion.audit.core.model.RequestInfo;
import io.orion.audit.core.spi.RequestInfoResolver;

/**
 * Fallback request info resolver.
 */
public class NoopRequestInfoResolver implements RequestInfoResolver {

    private final String defaultSource;

    public NoopRequestInfoResolver(String defaultSource) {
        this.defaultSource = defaultSource;
    }

    @Override
    public RequestInfo resolve() {
        return RequestInfo.empty(defaultSource);
    }
}
