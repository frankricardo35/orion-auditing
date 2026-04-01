package io.orion.audit.core.model;

/**
 * Controls which persistence listener integration is used for entity auditing.
 */
public enum AuditListenerMode {
    AUTO,
    HIBERNATE,
    JPA
}
