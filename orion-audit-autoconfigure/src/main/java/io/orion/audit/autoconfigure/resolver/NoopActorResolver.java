package io.orion.audit.autoconfigure.resolver;

import io.orion.audit.core.model.ActorInfo;
import io.orion.audit.core.spi.ActorResolver;

/**
 * Fallback actor resolver.
 */
public class NoopActorResolver implements ActorResolver {

    @Override
    public ActorInfo resolve() {
        return ActorInfo.anonymous();
    }
}
