# orion-audit

`orion-audit` is a Spring Boot auditing starter for JPA applications.

It lets you:

- mark an entity with `@Audited`
- automatically record `INSERT`, `UPDATE`, and `DELETE`
- store audit records in a database table
- capture actor and request metadata
- query audit records from your application
- resolve one or many actor types such as `users` and `customers`

This library is built as a starter, so application code should usually depend on `orion-audit-starter`.

## What You Get By Default

If you add the starter and annotate an entity:

- auditing is enabled automatically
- the database driver is enabled automatically
- the audit table name is `audit_log`
- the audit table is created automatically on startup if it does not already exist
- Spring Security actor resolution is used when available
- actor rows already store `actor_id`, `actor_name`, and `actor_type`
- HTTP request metadata is captured when available
- Hibernate listener mode is used when Hibernate is present

In other words, the normal happy path should work without extra setup.

## Requirements

- Java 17+
- Spring Boot 4.x
- Spring Data JPA
- A configured datasource

## Installation

### Maven

```xml
<dependency>
  <groupId>io.orion.audit</groupId>
  <artifactId>orion-audit-starter</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation("io.orion.audit:orion-audit-starter:1.0.0-SNAPSHOT")
```

## Fastest Way To Use It

### 1. Add the dependency

Use `orion-audit-starter`.

### 2. Annotate your entity

```java
import io.orion.audit.core.annotation.AuditIgnore;
import io.orion.audit.core.annotation.Audited;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@Audited(ignore = {"password"}, label = "Customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    @AuditIgnore
    private String password;
}
```

### 3. Start your application

That is enough for a basic setup.

When your app starts:

- the library auto-configures itself
- the audit table is created if it does not exist
- entity changes start being recorded

## Minimal Configuration

You do not need any custom properties for the default setup.

If you want to be explicit, this is a good starting point:

```properties
orion.audit.enabled=true
orion.audit.database-enabled=true
orion.audit.initialize-schema=true
orion.audit.table-name=audit_log
orion.audit.store-full-snapshot=false
orion.audit.store-empty-changes=false
orion.audit.fail-on-error=false
orion.audit.use-spring-security=true
orion.audit.capture-request-info=true
orion.audit.default-source=application
orion.audit.prefer-json-column=true
```

## Common Configuration Examples

### Change the audit table name

```properties
orion.audit.table-name=audit_logs
```

### Ignore fields globally

```properties
orion.audit.ignored-fields=password,createdAt,updatedAt
```

### Force JPA listener mode

```properties
orion.audit.listener-mode=jpa
```

### Force Hibernate listener mode

```properties
orion.audit.listener-mode=hibernate
```

### Write to database and application logs

```properties
orion.audit.drivers[0]=DATABASE
orion.audit.drivers[1]=LOGGING
```

### Configure one actor type

```properties
orion.audit.actors[0].type=users
orion.audit.actors[0].principal-class=com.example.security.UserPrincipal
orion.audit.actors[0].id-property=id
orion.audit.actors[0].name-property=fullName
```

### Configure multiple actor types

```properties
orion.audit.actors[0].type=users
orion.audit.actors[0].principal-class=com.example.security.UserPrincipal
orion.audit.actors[0].id-property=id
orion.audit.actors[0].name-property=fullName

orion.audit.actors[1].type=customers
orion.audit.actors[1].principal-class=com.example.customer.CustomerPrincipal
orion.audit.actors[1].id-property=customerId
orion.audit.actors[1].name-property=displayName
```

### Use a schema

```properties
orion.audit.schema=audit
orion.audit.table-name=audit_log
```

## How The Table Is Created

There are two supported ways.

### Option 1. Automatic startup creation

This is the default behavior.

If the audit table does not exist, the library creates it automatically on startup.

This is controlled by:

```properties
orion.audit.initialize-schema=true
```

This is the easiest option if you just want the library to work.

### Option 2. Flyway-managed creation

The library also ships vendor-specific Flyway migrations.

Locations:

- `classpath:META-INF/orion-audit/db/migration/postgresql`
- `classpath:META-INF/orion-audit/db/migration/mysql`
- `classpath:META-INF/orion-audit/db/migration/h2`

Use this option if your project wants audit table creation to be fully managed by Flyway.

Important:

- if you do not override `spring.flyway.locations`, the library can contribute its migration location automatically
- if you do override `spring.flyway.locations`, you must include the audit migration location yourself

Example:

```properties
spring.flyway.locations=classpath:db/migration,classpath:META-INF/orion-audit/db/migration/postgresql
```

## Important Properties

| Property | Default | Meaning |
| --- | --- | --- |
| `orion.audit.enabled` | `true` | Turns the library on or off |
| `orion.audit.database-enabled` | `true` | Keeps the database driver enabled |
| `orion.audit.initialize-schema` | `true` | Creates the audit table automatically if missing |
| `orion.audit.listener-mode` | `AUTO` | `AUTO`, `HIBERNATE`, or `JPA` |
| `orion.audit.table-name` | `audit_log` | Audit table name |
| `orion.audit.schema` | empty | Optional schema |
| `orion.audit.store-full-snapshot` | `false` | Store full old/new maps instead of changed-only maps |
| `orion.audit.store-empty-changes` | `false` | Store update audits even if nothing changed |
| `orion.audit.fail-on-error` | `false` | Throw on audit failure instead of logging only |
| `orion.audit.ignored-fields` | empty | Globally ignored field names |
| `orion.audit.use-spring-security` | `true` | Resolve actor from Spring Security |
| `orion.audit.capture-request-info` | `true` | Capture URI, method, IP, user agent, trace ID |
| `orion.audit.default-source` | `application` | Default source label |
| `orion.audit.prefer-json-column` | `true` | Prefer JSON-capable database column types |
| `orion.audit.actors` | empty | Optional Spring Security actor mappings for one or many actor types |
| `orion.audit.flyway.enabled` | `true` | Enable audit Flyway contribution |
| `orion.audit.flyway.vendor` | `auto` | Vendor override for audit migrations |
| `orion.audit.flyway.append-to-existing` | `true` | Append audit Flyway locations to existing locations |
| `orion.audit.flyway.locations` | empty | Extra audit migration locations |

## Polymorphic Actors

Laravel Auditing uses a morph-style `user_type` and `user_id` approach. In `orion-audit`, the equivalent columns are:

- `actor_type`
- `actor_id`
- `actor_name`

This means one audit table can represent actions from different actor families such as:

- `users`
- `customers`
- `admins`

If you configure `orion.audit.actors`, the Spring Security resolver will match the authenticated principal class and populate those columns from the configured property paths.

Example:

```yaml
orion:
  audit:
    actors:
      - type: users
        principal-class: com.example.security.UserPrincipal
        id-property: id
        name-property: fullName
      - type: customers
        principal-class: com.example.customer.CustomerPrincipal
        id-property: customerId
        name-property: displayName
```

Notes:

- `type` is what gets stored in `actor_type`
- `principal-class` matches the authenticated Spring Security principal
- `id-property` and `name-property` are bean property paths read from that principal
- if you do not configure actors, the library keeps the old fallback behavior and uses `authentication.getName()`
- if you configure one actor mapping without `principal-class`, it works as a default mapping for any authenticated principal

## Annotations

### `@Audited`

Use it on an entity class.

Example:

```java
@Audited(
    actions = {AuditAction.INSERT, AuditAction.UPDATE, AuditAction.DELETE},
    ignore = {"password"},
    include = {},
    label = "Customer",
    storeFullSnapshot = false
)
```

### `@AuditIgnore`

Use it on a field you never want in the audit payload.

```java
@AuditIgnore
private String password;
```

### `@AuditField`

Use it to rename a field in the audit output.

```java
@AuditField("fullName")
private String name;
```

## What Gets Stored

Each audit row contains:

- `id`
- `action`
- `entity_type`
- `entity_name`
- `entity_id`
- `old_values`
- `new_values`
- `changed_fields`
- `actor_id`
- `actor_name`
- `actor_type`
- `ip_address`
- `user_agent`
- `request_uri`
- `http_method`
- `trace_id`
- `source`
- `tenant_id`
- `tags`
- `created_at`

## Reading Audit Records

Use `AuditQueryService`.

Example:

```java
import io.orion.audit.core.model.AuditAction;
import io.orion.audit.core.query.AuditPageRequest;
import io.orion.audit.core.query.AuditQueryCriteria;
import io.orion.audit.core.query.AuditQueryService;

AuditQueryCriteria criteria = AuditQueryCriteria.builder()
    .entityType("com.example.Customer")
    .entityId("42")
    .actions(Set.of(AuditAction.UPDATE, AuditAction.DELETE))
    .build();

var page = auditQueryService.find(criteria, AuditPageRequest.of(0, 20));
```

Supported filters:

- `entityType`
- `entityId`
- `actions`
- `createdFrom`
- `createdTo`
- `actorId`
- `actorType`
- `source`
- `tenantId`

Default ordering is `createdAt DESC`.

## Extending The Library

### Custom tags

```java
@Component
@Order(10)
class RoleTagResolver implements TagResolver {
    @Override
    public Collection<String> resolveTags(AuditEntry entry) {
        return List.of("role:admin");
    }
}
```

### Modify entries before storage

```java
@Component
@Order(20)
class SourceModifier implements AuditModifier {
    @Override
    public AuditEntry modify(AuditEntry entry) {
        return entry.toBuilder().source("batch-job").build();
    }
}
```

### Add tenant resolution

```java
@Component
class CurrentTenantResolver implements TenantResolver {
    @Override
    public TenantContext resolveTenant() {
        return TenantContext.of("tenant-a");
    }
}
```

## Driver Options

### Database driver

This is the default driver.

### Logging driver

This writes audit entries as structured JSON to logger:

```text
io.orion.audit.log
```

### Multiple drivers

```properties
orion.audit.drivers[0]=DATABASE
orion.audit.drivers[1]=LOGGING
```

## Demo

Run the demo app:

```bash
env -u JAVA_HOME mvn -pl orion-audit-demo spring-boot:run
```

Example calls:

```bash
curl -X POST http://localhost:8080/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com","password":"secret"}'

curl -X PUT http://localhost:8080/customers/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice Smith","email":"alice.smith@example.com","password":"new-secret"}'

curl -X DELETE http://localhost:8080/customers/1

curl http://localhost:8080/audit-logs
```

## Troubleshooting

### The properties are not recognized in my IDE

Make sure your application is using the latest built version of the library.

If you are using a local snapshot:

```bash
env -u JAVA_HOME mvn clean install
```

### The audit table is not created

Check:

- `orion.audit.enabled=true`
- `orion.audit.database-enabled=true`
- `orion.audit.initialize-schema=true`
- your datasource is configured

If you want Flyway to manage the table instead, include the library migration location.

### I want different actor types like users and customers

Configure `orion.audit.actors` and map each Spring Security principal class to the stored `actor_type`, `actor_id`, and `actor_name`.

If your table already exists, no schema change is required because the library already stores:

- `actor_id`
- `actor_name`
- `actor_type`

### Audits are not being written

Check:

- your entity is annotated with `@Audited`
- the entity is actually managed by JPA
- you are performing insert, update, or delete through JPA/Hibernate
- auditing is enabled

## Build

```bash
env -u JAVA_HOME mvn clean verify
```

The project targets Java 17+ and Spring Boot 4.x.
