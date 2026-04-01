package io.orion.audit.autoconfigure.driver;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.orion.audit.autoconfigure.support.JacksonValueSerializer;
import io.orion.audit.core.model.AuditAction;
import io.orion.audit.core.model.AuditEntry;
import io.orion.audit.core.spi.AuditDriver;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditDriverExtensionsTest {

    @Test
    void shouldLogStructuredJsonAuditEntries() {
        Logger logger = (Logger) LoggerFactory.getLogger("io.orion.audit.log");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            LoggingAuditDriver driver = new LoggingAuditDriver(new JacksonValueSerializer(new ObjectMapper().findAndRegisterModules()));
            driver.save(sampleEntry());

            assertThat(appender.list).isNotEmpty();
            assertThat(appender.list.get(0).getFormattedMessage()).contains("\"entityType\":\"io.orion.Customer\"");
            assertThat(appender.list.get(0).getFormattedMessage()).contains("\"action\":\"INSERT\"");
        }
        finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldDelegateToMultipleDriversInOrder() {
        List<String> calls = new ArrayList<>();
        AuditDriver first = entry -> calls.add("first:" + entry.getId());
        AuditDriver second = entry -> calls.add("second:" + entry.getId());

        CompositeAuditDriver driver = new CompositeAuditDriver(List.of(first, second));
        driver.save(sampleEntry());

        assertThat(calls).containsExactly("first:1", "second:1");
    }

    private AuditEntry sampleEntry() {
        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("old", null);
        delta.put("new", "Alice");
        return AuditEntry.builder()
            .id("1")
            .action(AuditAction.INSERT)
            .entityType("io.orion.Customer")
            .entityName("Customer")
            .entityId("99")
            .newValues(Map.of("name", "Alice"))
            .changedFields(Map.of("name", delta))
            .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
            .build();
    }
}
