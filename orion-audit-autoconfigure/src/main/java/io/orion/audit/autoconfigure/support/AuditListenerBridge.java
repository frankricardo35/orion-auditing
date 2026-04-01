package io.orion.audit.autoconfigure.support;

import io.orion.audit.autoconfigure.service.AuditOrchestrator;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Static bridge used by the JPA entity listener.
 */
public final class AuditListenerBridge {

    private static final AtomicReference<AuditOrchestrator> ORCHESTRATOR = new AtomicReference<>();
    private static final AtomicReference<AuditListenerModeResolver> MODE_RESOLVER = new AtomicReference<>();

    private AuditListenerBridge() {
    }

    public static void set(AuditOrchestrator orchestrator) {
        ORCHESTRATOR.set(orchestrator);
    }

    public static AuditOrchestrator get() {
        return ORCHESTRATOR.get();
    }

    public static void setModeResolver(AuditListenerModeResolver modeResolver) {
        MODE_RESOLVER.set(modeResolver);
    }

    public static AuditListenerModeResolver getModeResolver() {
        return MODE_RESOLVER.get();
    }
}
