package io.orion.audit.autoconfigure.properties;

import io.orion.audit.core.model.AuditAction;
import io.orion.audit.core.model.AuditListenerMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration properties for orion-audit.
 */
@ConfigurationProperties(prefix = "orion.audit")
public class AuditProperties {

    private boolean enabled = true;
    private boolean databaseEnabled = true;
    private AuditListenerMode listenerMode = AuditListenerMode.AUTO;
    private Set<AuditAction> actions = EnumSet.of(AuditAction.INSERT, AuditAction.UPDATE, AuditAction.DELETE);
    private List<AuditDriverType> drivers = new ArrayList<>();
    private String tableName = "audit_log";
    private String schema;
    private boolean storeFullSnapshot = false;
    private boolean storeEmptyChanges = false;
    private boolean failOnError = false;
    private List<String> ignoredFields = new ArrayList<>();
    private boolean useSpringSecurity = true;
    private boolean captureRequestInfo = true;
    private String defaultSource = "application";
    private boolean preferJsonColumn = true;
    private boolean initializeSchema = true;
    private String ddlAuto = "none";
    private EntityTypeFormat entityTypeFormat = EntityTypeFormat.QUALIFIED;
    private List<Actor> actors = new ArrayList<>();
    private Flyway flyway = new Flyway();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDatabaseEnabled() {
        return databaseEnabled;
    }

    public void setDatabaseEnabled(boolean databaseEnabled) {
        this.databaseEnabled = databaseEnabled;
    }

    public AuditListenerMode getListenerMode() {
        return listenerMode;
    }

    public void setListenerMode(AuditListenerMode listenerMode) {
        this.listenerMode = listenerMode;
    }

    public Set<AuditAction> getActions() {
        return actions;
    }

    public void setActions(Set<AuditAction> actions) {
        this.actions = actions;
    }

    public List<AuditDriverType> getDrivers() {
        return drivers;
    }

    public void setDrivers(List<AuditDriverType> drivers) {
        this.drivers = drivers;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public boolean isStoreFullSnapshot() {
        return storeFullSnapshot;
    }

    public void setStoreFullSnapshot(boolean storeFullSnapshot) {
        this.storeFullSnapshot = storeFullSnapshot;
    }

    public boolean isStoreEmptyChanges() {
        return storeEmptyChanges;
    }

    public void setStoreEmptyChanges(boolean storeEmptyChanges) {
        this.storeEmptyChanges = storeEmptyChanges;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public List<String> getIgnoredFields() {
        return ignoredFields;
    }

    public void setIgnoredFields(List<String> ignoredFields) {
        this.ignoredFields = ignoredFields;
    }

    public boolean isUseSpringSecurity() {
        return useSpringSecurity;
    }

    public void setUseSpringSecurity(boolean useSpringSecurity) {
        this.useSpringSecurity = useSpringSecurity;
    }

    public boolean isCaptureRequestInfo() {
        return captureRequestInfo;
    }

    public void setCaptureRequestInfo(boolean captureRequestInfo) {
        this.captureRequestInfo = captureRequestInfo;
    }

    public String getDefaultSource() {
        return defaultSource;
    }

    public void setDefaultSource(String defaultSource) {
        this.defaultSource = defaultSource;
    }

    public boolean isPreferJsonColumn() {
        return preferJsonColumn;
    }

    public void setPreferJsonColumn(boolean preferJsonColumn) {
        this.preferJsonColumn = preferJsonColumn;
    }

    public boolean isInitializeSchema() {
        return initializeSchema;
    }

    public void setInitializeSchema(boolean initializeSchema) {
        this.initializeSchema = initializeSchema;
    }

    public String getDdlAuto() {
        return ddlAuto;
    }

    public void setDdlAuto(String ddlAuto) {
        this.ddlAuto = ddlAuto;
    }

    public EntityTypeFormat getEntityTypeFormat() {
        return entityTypeFormat;
    }

    public void setEntityTypeFormat(EntityTypeFormat entityTypeFormat) {
        this.entityTypeFormat = entityTypeFormat;
    }

    public List<Actor> getActors() {
        return actors;
    }

    public void setActors(List<Actor> actors) {
        this.actors = actors;
    }

    public Flyway getFlyway() {
        return flyway;
    }

    public void setFlyway(Flyway flyway) {
        this.flyway = flyway;
    }

    public boolean isDatabaseDriverEnabled() {
        if (!drivers.isEmpty()) {
            return drivers.contains(AuditDriverType.DATABASE);
        }
        return databaseEnabled;
    }

    public boolean isLoggingDriverEnabled() {
        return drivers.contains(AuditDriverType.LOGGING);
    }

    public static class Flyway {

        private boolean enabled = true;
        private String vendor = "auto";
        private List<String> locations = new ArrayList<>();
        private boolean appendToExisting = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getVendor() {
            return vendor;
        }

        public void setVendor(String vendor) {
            this.vendor = vendor;
        }

        public List<String> getLocations() {
            return locations;
        }

        public void setLocations(List<String> locations) {
            this.locations = locations;
        }

        public boolean isAppendToExisting() {
            return appendToExisting;
        }

        public void setAppendToExisting(boolean appendToExisting) {
            this.appendToExisting = appendToExisting;
        }
    }

    public static class Actor {

        private String type;
        private String principalClass;
        private String idProperty = "id";
        private String nameProperty = "name";
        private String tenantIdProperty;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPrincipalClass() {
            return principalClass;
        }

        public void setPrincipalClass(String principalClass) {
            this.principalClass = principalClass;
        }

        public String getIdProperty() {
            return idProperty;
        }

        public void setIdProperty(String idProperty) {
            this.idProperty = idProperty;
        }

        public String getNameProperty() {
            return nameProperty;
        }

        public void setNameProperty(String nameProperty) {
            this.nameProperty = nameProperty;
        }

        public String getTenantIdProperty() {
            return tenantIdProperty;
        }

        public void setTenantIdProperty(String tenantIdProperty) {
            this.tenantIdProperty = tenantIdProperty;
        }
    }
}
