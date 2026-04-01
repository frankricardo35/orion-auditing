package io.orion.audit.autoconfigure.listener;

import io.orion.audit.autoconfigure.service.AuditOrchestrator;
import io.orion.audit.autoconfigure.support.AuditListenerBridge;

/**
 * Global JPA listener for auditable entities.
 */
public class AuditEntityListener {

    public void onPostLoad(Object entity) {
        orchestrator().ifPresent(manager -> manager.onPostLoad(entity));
    }

    public void onPostPersist(Object entity) {
        orchestrator().ifPresent(manager -> manager.onPostPersist(entity));
    }

    public void onPreUpdate(Object entity) {
        orchestrator().ifPresent(manager -> manager.onPreUpdate(entity));
    }

    public void onPostUpdate(Object entity) {
        orchestrator().ifPresent(manager -> manager.onPostUpdate(entity));
    }

    public void onPreRemove(Object entity) {
        orchestrator().ifPresent(manager -> manager.onPreRemove(entity));
    }

    public void onPostRemove(Object entity) {
        orchestrator().ifPresent(manager -> manager.onPostRemove(entity));
    }

    private java.util.Optional<AuditOrchestrator> orchestrator() {
        if (AuditListenerBridge.getModeResolver() != null && !AuditListenerBridge.getModeResolver().isJpaListenerActive()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(AuditListenerBridge.get());
    }
}
