package io.orion.audit.core.query;

import io.orion.audit.core.model.AuditAction;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Filtering criteria for querying audit records.
 */
public final class AuditQueryCriteria {

    private final String entityType;
    private final String entityId;
    private final Set<AuditAction> actions;
    private final Instant createdFrom;
    private final Instant createdTo;
    private final String actorId;
    private final String actorType;
    private final String source;
    private final String tenantId;

    private AuditQueryCriteria(Builder builder) {
        this.entityType = builder.entityType;
        this.entityId = builder.entityId;
        this.actions = builder.actions.isEmpty() ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(builder.actions));
        this.createdFrom = builder.createdFrom;
        this.createdTo = builder.createdTo;
        this.actorId = builder.actorId;
        this.actorType = builder.actorType;
        this.source = builder.source;
        this.tenantId = builder.tenantId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public Set<AuditAction> getActions() {
        return actions;
    }

    public Instant getCreatedFrom() {
        return createdFrom;
    }

    public Instant getCreatedTo() {
        return createdTo;
    }

    public String getActorId() {
        return actorId;
    }

    public String getActorType() {
        return actorType;
    }

    public String getSource() {
        return source;
    }

    public String getTenantId() {
        return tenantId;
    }

    public static final class Builder {

        private String entityType;
        private String entityId;
        private Set<AuditAction> actions = new LinkedHashSet<>();
        private Instant createdFrom;
        private Instant createdTo;
        private String actorId;
        private String actorType;
        private String source;
        private String tenantId;

        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder actions(Set<AuditAction> actions) {
            this.actions = actions == null ? new LinkedHashSet<>() : new LinkedHashSet<>(actions);
            return this;
        }

        public Builder createdFrom(Instant createdFrom) {
            this.createdFrom = createdFrom;
            return this;
        }

        public Builder createdTo(Instant createdTo) {
            this.createdTo = createdTo;
            return this;
        }

        public Builder actorId(String actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder actorType(String actorType) {
            this.actorType = actorType;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public AuditQueryCriteria build() {
            return new AuditQueryCriteria(this);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuditQueryCriteria that)) {
            return false;
        }
        return Objects.equals(entityType, that.entityType)
            && Objects.equals(entityId, that.entityId)
            && Objects.equals(actions, that.actions)
            && Objects.equals(createdFrom, that.createdFrom)
            && Objects.equals(createdTo, that.createdTo)
            && Objects.equals(actorId, that.actorId)
            && Objects.equals(actorType, that.actorType)
            && Objects.equals(source, that.source)
            && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityType, entityId, actions, createdFrom, createdTo, actorId, actorType, source, tenantId);
    }
}
