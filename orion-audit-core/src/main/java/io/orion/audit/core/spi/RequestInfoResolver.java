package io.orion.audit.core.spi;

import io.orion.audit.core.model.RequestInfo;

/**
 * Resolves request metadata for the current execution context.
 */
public interface RequestInfoResolver {

    /**
     * @return request information, possibly empty
     */
    RequestInfo resolve();
}
