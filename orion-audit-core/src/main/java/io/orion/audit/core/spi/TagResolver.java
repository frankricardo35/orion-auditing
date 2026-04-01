package io.orion.audit.core.spi;

import io.orion.audit.core.model.AuditEntry;

import java.util.Collection;

/**
 * Contributes dynamic tags for an audit entry.
 */
public interface TagResolver {

    /**
     * Resolves tags for the supplied audit entry.
     *
     * @param entry current audit entry
     * @return zero or more tags
     */
    Collection<String> resolveTags(AuditEntry entry);
}
