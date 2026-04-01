package io.orion.audit.core.spi;

/**
 * Resolves an entity identifier as a string.
 */
public interface EntityIdResolver {

    /**
     * @param entity entity instance
     * @return resolved identifier, or {@code null} if unavailable
     */
    String resolveEntityId(Object entity);
}
