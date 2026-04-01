CREATE TABLE IF NOT EXISTS ${orion_audit_schema_prefix}${orion_audit_table} (
    id VARCHAR(36) PRIMARY KEY,
    action VARCHAR(32) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    entity_name VARCHAR(255) NOT NULL,
    entity_id VARCHAR(255),
    old_values JSONB,
    new_values JSONB,
    changed_fields JSONB,
    actor_id VARCHAR(255),
    actor_name VARCHAR(255),
    actor_type VARCHAR(255),
    ip_address VARCHAR(64),
    user_agent VARCHAR(1024),
    request_uri VARCHAR(1024),
    http_method VARCHAR(32),
    trace_id VARCHAR(255),
    source VARCHAR(128),
    tenant_id VARCHAR(255),
    tags JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_${orion_audit_table}_entity_type ON ${orion_audit_schema_prefix}${orion_audit_table} (entity_type);
CREATE INDEX IF NOT EXISTS idx_${orion_audit_table}_entity_id ON ${orion_audit_schema_prefix}${orion_audit_table} (entity_id);
CREATE INDEX IF NOT EXISTS idx_${orion_audit_table}_action ON ${orion_audit_schema_prefix}${orion_audit_table} (action);
CREATE INDEX IF NOT EXISTS idx_${orion_audit_table}_actor_type ON ${orion_audit_schema_prefix}${orion_audit_table} (actor_type);
CREATE INDEX IF NOT EXISTS idx_${orion_audit_table}_actor_id ON ${orion_audit_schema_prefix}${orion_audit_table} (actor_id);
CREATE INDEX IF NOT EXISTS idx_${orion_audit_table}_created_at ON ${orion_audit_schema_prefix}${orion_audit_table} (created_at);
