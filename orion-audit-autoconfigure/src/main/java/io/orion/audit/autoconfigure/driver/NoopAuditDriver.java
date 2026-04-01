package io.orion.audit.autoconfigure.driver;

import io.orion.audit.core.model.AuditEntry;
import io.orion.audit.core.spi.AuditDriver;

/**
 * Audit driver that intentionally discards audit entries.
 */
public class NoopAuditDriver implements AuditDriver {

    @Override
    public void save(AuditEntry entry) {
    }
}
