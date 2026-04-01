package io.orion.audit.core.spi;

import io.orion.audit.core.model.ActorInfo;

/**
 * Resolves the actor that triggered an audit event.
 */
public interface ActorResolver {

    /**
     * @return actor details or an anonymous actor if unavailable
     */
    ActorInfo resolve();
}
