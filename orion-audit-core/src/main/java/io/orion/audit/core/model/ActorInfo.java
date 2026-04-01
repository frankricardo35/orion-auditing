package io.orion.audit.core.model;

import java.util.Objects;

/**
 * Identity information about the actor that triggered the audited action.
 */
public final class ActorInfo {

    private final String actorId;
    private final String actorName;
    private final String actorType;
    private final String tenantId;

    public ActorInfo(String actorId, String actorName, String actorType, String tenantId) {
        this.actorId = actorId;
        this.actorName = actorName;
        this.actorType = actorType;
        this.tenantId = tenantId;
    }

    public static ActorInfo anonymous() {
        return new ActorInfo(null, null, null, null);
    }

    public String getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getActorType() {
        return actorType;
    }

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ActorInfo that)) {
            return false;
        }
        return Objects.equals(actorId, that.actorId)
            && Objects.equals(actorName, that.actorName)
            && Objects.equals(actorType, that.actorType)
            && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorId, actorName, actorType, tenantId);
    }
}
