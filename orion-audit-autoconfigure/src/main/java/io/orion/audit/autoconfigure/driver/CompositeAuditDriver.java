package io.orion.audit.autoconfigure.driver;

import io.orion.audit.core.model.AuditEntry;
import io.orion.audit.core.spi.AuditDriver;

import java.util.List;

/**
 * Delegates audit persistence to multiple drivers in order.
 */
public class CompositeAuditDriver implements AuditDriver {

    private final List<AuditDriver> delegates;

    public CompositeAuditDriver(List<AuditDriver> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public void save(AuditEntry entry) {
        for (AuditDriver delegate : delegates) {
            delegate.save(entry);
        }
    }
}
