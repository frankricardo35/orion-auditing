package io.orion.audit.core.annotation;

import io.orion.audit.core.model.AuditAction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JPA entity as auditable by orion-audit.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * Which actions should generate audit records.
     *
     * @return supported audit actions
     */
    AuditAction[] actions() default {
        AuditAction.INSERT,
        AuditAction.UPDATE,
        AuditAction.DELETE
    };

    /**
     * Fields to exclude from auditing for this entity.
     *
     * @return ignored field names
     */
    String[] ignore() default {};

    /**
     * If populated, only these fields are considered for auditing.
     *
     * @return included field names
     */
    String[] include() default {};

    /**
     * Optional human-readable label for the entity.
     *
     * @return label override
     */
    String label() default "";

    /**
     * Overrides the global full snapshot setting for this entity.
     *
     * @return whether to store the full entity snapshot
     */
    boolean storeFullSnapshot() default false;
}
