package io.orion.audit.autoconfigure.support;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Stores entity snapshots and pending entries for the current thread/transaction.
 */
public class AuditStateStore {

    private final ThreadLocal<IdentityHashMap<Object, Map<String, Object>>> snapshots =
        ThreadLocal.withInitial(IdentityHashMap::new);
    private final ThreadLocal<IdentityHashMap<Object, Runnable>> pending =
        ThreadLocal.withInitial(IdentityHashMap::new);
    private final ThreadLocal<IdentityHashMap<Object, Boolean>> inserted =
        ThreadLocal.withInitial(IdentityHashMap::new);
    private final ThreadLocal<Boolean> cleanupRegistered =
        ThreadLocal.withInitial(() -> Boolean.FALSE);

    public void putSnapshot(Object entity, Map<String, Object> values) {
        registerCleanupIfNeeded();
        snapshots.get().put(entity, values);
    }

    public Map<String, Object> getSnapshot(Object entity) {
        return snapshots.get().get(entity);
    }

    public Map<String, Object> removeSnapshot(Object entity) {
        return snapshots.get().remove(entity);
    }

    public void putPending(Object entity, Runnable task) {
        registerCleanupIfNeeded();
        pending.get().put(entity, task);
    }

    public Runnable removePending(Object entity) {
        return pending.get().remove(entity);
    }

    public boolean markInserted(Object entity) {
        registerCleanupIfNeeded();
        Boolean previous = inserted.get().put(entity, Boolean.TRUE);
        return previous == null;
    }

    public void clear() {
        snapshots.get().clear();
        pending.get().clear();
        inserted.get().clear();
        cleanupRegistered.remove();
        snapshots.remove();
        pending.remove();
        inserted.remove();
    }

    private void registerCleanupIfNeeded() {
        if (Boolean.TRUE.equals(cleanupRegistered.get())) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            cleanupRegistered.set(Boolean.TRUE);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    clear();
                }
            });
        }
    }
}
