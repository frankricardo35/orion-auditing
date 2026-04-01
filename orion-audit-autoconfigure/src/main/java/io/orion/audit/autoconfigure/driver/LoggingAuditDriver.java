package io.orion.audit.autoconfigure.driver;

import io.orion.audit.core.model.AuditEntry;
import io.orion.audit.core.spi.AuditDriver;
import io.orion.audit.core.spi.ValueSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emits audit entries as structured JSON log lines.
 */
public class LoggingAuditDriver implements AuditDriver {

    private static final Logger log = LoggerFactory.getLogger("io.orion.audit.log");

    private final ValueSerializer valueSerializer;

    public LoggingAuditDriver(ValueSerializer valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public void save(AuditEntry entry) {
        log.info("{}", valueSerializer.serialize(entry));
    }
}
