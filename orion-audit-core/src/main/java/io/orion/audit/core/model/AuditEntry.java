package io.orion.audit.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable audit entry that can be persisted by a driver.
 */
public final class AuditEntry {

    private final String id;
    private final AuditAction action;
    private final String entityType;
    private final String entityName;
    private final String entityId;
    private final Map<String, Object> oldValues;
    private final Map<String, Object> newValues;
    private final Map<String, Object> changedFields;
    private final String actorId;
    private final String actorName;
    private final String actorType;
    private final String ipAddress;
    private final String userAgent;
    private final String requestUri;
    private final String httpMethod;
    private final String traceId;
    private final String source;
    private final String tenantId;
    private final List<String> tags;
    private final Instant createdAt;

    private AuditEntry(Builder builder) {
        this.id = builder.id;
        this.action = Objects.requireNonNull(builder.action, "action must not be null");
        this.entityType = builder.entityType;
        this.entityName = builder.entityName;
        this.entityId = builder.entityId;
        this.oldValues = immutableMap(builder.oldValues);
        this.newValues = immutableMap(builder.newValues);
        this.changedFields = immutableMap(builder.changedFields);
        this.actorId = builder.actorId;
        this.actorName = builder.actorName;
        this.actorType = builder.actorType;
        this.ipAddress = builder.ipAddress;
        this.userAgent = builder.userAgent;
        this.requestUri = builder.requestUri;
        this.httpMethod = builder.httpMethod;
        this.traceId = builder.traceId;
        this.source = builder.source;
        this.tenantId = builder.tenantId;
        this.tags = immutableList(builder.tags);
        this.createdAt = builder.createdAt == null ? Instant.now() : builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
            .id(this.id)
            .action(this.action)
            .entityType(this.entityType)
            .entityName(this.entityName)
            .entityId(this.entityId)
            .oldValues(this.oldValues)
            .newValues(this.newValues)
            .changedFields(this.changedFields)
            .actorId(this.actorId)
            .actorName(this.actorName)
            .actorType(this.actorType)
            .ipAddress(this.ipAddress)
            .userAgent(this.userAgent)
            .requestUri(this.requestUri)
            .httpMethod(this.httpMethod)
            .traceId(this.traceId)
            .source(this.source)
            .tenantId(this.tenantId)
            .tags(this.tags)
            .createdAt(this.createdAt);
    }

    public String getId() {
        return id;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getEntityId() {
        return entityId;
    }

    public Map<String, Object> getOldValues() {
        return oldValues;
    }

    public Map<String, Object> getNewValues() {
        return newValues;
    }

    public Map<String, Object> getChangedFields() {
        return changedFields;
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

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSource() {
        return source;
    }

    public String getTenantId() {
        return tenantId;
    }

    public List<String> getTags() {
        return tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static final class Builder {

        private String id;
        private AuditAction action;
        private String entityType;
        private String entityName;
        private String entityId;
        private Map<String, Object> oldValues;
        private Map<String, Object> newValues;
        private Map<String, Object> changedFields;
        private String actorId;
        private String actorName;
        private String actorType;
        private String ipAddress;
        private String userAgent;
        private String requestUri;
        private String httpMethod;
        private String traceId;
        private String source;
        private String tenantId;
        private List<String> tags;
        private Instant createdAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder action(AuditAction action) {
            this.action = action;
            return this;
        }

        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder entityName(String entityName) {
            this.entityName = entityName;
            return this;
        }

        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder oldValues(Map<String, Object> oldValues) {
            this.oldValues = oldValues;
            return this;
        }

        public Builder newValues(Map<String, Object> newValues) {
            this.newValues = newValues;
            return this;
        }

        public Builder changedFields(Map<String, Object> changedFields) {
            this.changedFields = changedFields;
            return this;
        }

        public Builder actorId(String actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder actorName(String actorName) {
            this.actorName = actorName;
            return this;
        }

        public Builder actorType(String actorType) {
            this.actorType = actorType;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder requestUri(String requestUri) {
            this.requestUri = requestUri;
            return this;
        }

        public Builder httpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
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

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AuditEntry build() {
            return new AuditEntry(this);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> immutableMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), immutableValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static List<String> immutableList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    @SuppressWarnings("unchecked")
    private static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                nested.put(String.valueOf(entry.getKey()), immutableValue(entry.getValue()));
            }
            return Collections.unmodifiableMap(nested);
        }
        if (value instanceof List<?> listValue) {
            List<Object> nested = new ArrayList<>();
            for (Object item : listValue) {
                nested.add(immutableValue(item));
            }
            return Collections.unmodifiableList(nested);
        }
        return value;
    }
}
