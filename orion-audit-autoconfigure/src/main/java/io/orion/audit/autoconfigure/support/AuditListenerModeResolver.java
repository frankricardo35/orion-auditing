package io.orion.audit.autoconfigure.support;

import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.core.model.AuditListenerMode;
import org.springframework.util.ClassUtils;

/**
 * Resolves the effective audit listener mode for the running application.
 */
public class AuditListenerModeResolver {

    private static final String HIBERNATE_LISTENER_CLASS = "org.hibernate.event.spi.PostInsertEventListener";

    private final boolean hibernateAvailable;
    private final AuditListenerMode effectiveMode;

    public AuditListenerModeResolver(AuditProperties properties, ClassLoader classLoader) {
        this.hibernateAvailable = ClassUtils.isPresent(HIBERNATE_LISTENER_CLASS, classLoader);
        AuditListenerMode configuredMode = properties.getListenerMode() == null ? AuditListenerMode.AUTO : properties.getListenerMode();
        if (configuredMode == AuditListenerMode.HIBERNATE && !hibernateAvailable) {
            throw new IllegalStateException("orion.audit.listener-mode=hibernate requires Hibernate event listener APIs on the classpath");
        }
        this.effectiveMode = configuredMode == AuditListenerMode.AUTO
            ? (hibernateAvailable ? AuditListenerMode.HIBERNATE : AuditListenerMode.JPA)
            : configuredMode;
    }

    public boolean isHibernateAvailable() {
        return hibernateAvailable;
    }

    public AuditListenerMode getEffectiveMode() {
        return effectiveMode;
    }

    public boolean isJpaListenerActive() {
        return effectiveMode == AuditListenerMode.JPA;
    }

    public boolean isHibernateListenerActive() {
        return effectiveMode == AuditListenerMode.HIBERNATE;
    }
}
