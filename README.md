# orion-audit

`orion-audit` is a Spring Boot auditing starter for JPA applications.

It is inspired by Laravel Auditing’s model-level auditing approach and brings the same idea to Spring Boot starter conventions.

With `orion-audit`, you can:

- mark entities with `@Audited`
- automatically record `INSERT`, `UPDATE`, and `DELETE`
- capture field-level changes and metadata
- persist audit rows in a database table
- query audit records from application code
- resolve one actor type or many actor types such as `users` and `customers`
- use Hibernate listeners for better dirty tracking while keeping JPA fallback support

## Table Of Contents

- [What You Get By Default](#what-you-get-by-default)
- [Requirements](#requirements)
- [Installation](#installation)
- [Maven](#maven)
- [Gradle](#gradle)
- [Publishing](#publishing)
- [Consuming From GitHub Packages](#consuming-from-github-packages)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Copy-Paste Starter Configs](#copy-paste-starter-configs)
- [Common Configuration Examples](#common-configuration-examples)
- [Polymorphic Actors](#polymorphic-actors)
- [How Table Creation Works](#how-table-creation-works)
- [Configuration Reference](#configuration-reference)
- [Annotations](#annotations)
- [Stored Audit Fields](#stored-audit-fields)
- [Reading Audit Records](#reading-audit-records)
- [Extension Points](#extension-points)
- [Driver Options](#driver-options)
- [Demo](#demo)
- [Troubleshooting](#troubleshooting)
- [Build](#build)

## What You Get By Default

If you add the starter and annotate an entity:

- auditing is enabled automatically
- the database driver is enabled automatically
- the audit table name is `audit_log`
- the audit table is created automatically on startup if it does not already exist
- Spring Security actor resolution is used when available
- actor rows store `actor_id`, `actor_name`, and `actor_type`
- HTTP request metadata is captured when available
- trace id is resolved from common tracing headers and MDC when available
- tenant id is resolved from request headers, security principal, or MDC when available
- Hibernate listener mode is used when Hibernate is present

The normal happy path should work without extra setup.

## Requirements

- Java 17+
- Spring Boot 4.x
- Spring Data JPA
- a configured datasource

## Installation

Application code should usually depend on `orion-audit-starter`.

## Maven

### Maven dependency

```xml
<dependency>
  <groupId>io.orion.audit</groupId>
  <artifactId>orion-audit-starter</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Maven snapshot repository

Use this only while consuming a local or unpublished snapshot from a Maven repository manager.

```xml
<repositories>
  <repository>
    <id>snapshots</id>
    <url>https://your-repository.example.com/maven-snapshots</url>
  </repository>
</repositories>
```

### Maven local snapshot usage

If you are consuming the library from your local machine:

```bash
cd /absolute/path/to/orion-audit
env -u JAVA_HOME mvn clean install
```

Then use the same version in your application `pom.xml`.

## Gradle

### Gradle Groovy DSL

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.orion.audit:orion-audit-starter:1.0.0-SNAPSHOT")
}
```

### Gradle Kotlin DSL

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.orion.audit:orion-audit-starter:1.0.0-SNAPSHOT")
}
```

### Gradle snapshot repository

Use this only if the artifact is coming from a snapshot repository.

Groovy DSL:

```groovy
repositories {
    mavenCentral()
    maven { url = uri("https://your-repository.example.com/maven-snapshots") }
}
```

Kotlin DSL:

```kotlin
repositories {
    mavenCentral()
    maven(url = "https://your-repository.example.com/maven-snapshots")
}
```

### Gradle local snapshot usage

If the snapshot was installed with Maven on the same machine, Gradle can read it from `mavenLocal()`.

Groovy DSL:

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}
```

Kotlin DSL:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}
```

## Publishing

This project is configured to publish to GitHub Packages.

Configured package repository:

- `https://maven.pkg.github.com/frankricardo35/orion-auditing`

### 1. Create a GitHub personal access token

Use a GitHub token that can publish and read packages for the target repository.

Typical permissions:

- `write:packages`
- `read:packages`
- `repo`

### 2. Add Maven credentials

Create or update `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

The server id must be `github` because that matches this project’s `distributionManagement`.

### 3. Publish the library

From the project root:

```bash
env -u JAVA_HOME mvn clean deploy
```

This publishes:

- `orion-audit-core`
- `orion-audit-autoconfigure`
- `orion-audit-starter`

The demo module is part of the build, but the starter remains the main consumer entry point.

### 4. Versioning note

If other projects are going to consume the package from GitHub Packages, prefer a real release version instead of only `-SNAPSHOT` builds when possible.

Example:

```xml
<version>1.0.0</version>
```

## Consuming From GitHub Packages

Another project cannot consume this library from a normal GitHub source repository URL alone. It must use a package repository such as GitHub Packages, Maven Central, or JitPack.

This project is currently set up for GitHub Packages.

### Maven consumer setup

Add the repository:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/frankricardo35/orion-auditing</url>
  </repository>
</repositories>
```

Add the dependency:

```xml
<dependency>
  <groupId>io.orion.audit</groupId>
  <artifactId>orion-audit-starter</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

The consuming machine also needs a matching `~/.m2/settings.xml` entry:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

### Gradle consumer setup

Groovy DSL:

```groovy
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/frankricardo35/orion-auditing")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.orion.audit:orion-audit-starter:1.0.0-SNAPSHOT")
}
```

Kotlin DSL:

```kotlin
repositories {
    mavenCentral()
    maven(url = "https://maven.pkg.github.com/frankricardo35/orion-auditing") {
        credentials {
            username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.orion.audit:orion-audit-starter:1.0.0-SNAPSHOT")
}
```

You can put credentials in `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

### Recommended consumer dependency

Use:

- `io.orion.audit:orion-audit-starter`

Do not depend on `orion-audit-demo`.

Use `orion-audit-core` or `orion-audit-autoconfigure` directly only if you intentionally want a lower-level integration.

## Quick Start

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

When your application starts:

- the library auto-configures itself
- the audit table is created if it does not exist
- entity changes start being recorded

## Configuration

You do not need custom properties for the default setup.

If you want an explicit starting point, use:

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
orion.audit.entity-type-format=qualified
orion.audit.default-source=application
orion.audit.prefer-json-column=true
```

Equivalent `application.yml`:

```yaml
orion:
  audit:
    enabled: true
    database-enabled: true
    initialize-schema: true
    table-name: audit_log
    store-full-snapshot: false
    store-empty-changes: false
    fail-on-error: false
    use-spring-security: true
    capture-request-info: true
    entity-type-format: qualified
    default-source: application
    prefer-json-column: true
```

## Copy-Paste Starter Configs

These examples are meant to be a fast starting point for new applications.

### PostgreSQL

`application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/appdb
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=none

orion.audit.enabled=true
orion.audit.database-enabled=true
orion.audit.initialize-schema=true
orion.audit.table-name=audit_log
orion.audit.listener-mode=auto
orion.audit.use-spring-security=true
orion.audit.capture-request-info=true
orion.audit.entity-type-format=qualified
orion.audit.default-source=application
orion.audit.prefer-json-column=true
```

`application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/appdb
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: none

orion:
  audit:
    enabled: true
    database-enabled: true
    initialize-schema: true
    table-name: audit_log
    listener-mode: auto
    use-spring-security: true
    capture-request-info: true
    entity-type-format: qualified
    default-source: application
    prefer-json-column: true
```

### MySQL

`application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/appdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=none

orion.audit.enabled=true
orion.audit.database-enabled=true
orion.audit.initialize-schema=true
orion.audit.table-name=audit_log
orion.audit.listener-mode=auto
orion.audit.use-spring-security=true
orion.audit.capture-request-info=true
orion.audit.entity-type-format=qualified
orion.audit.default-source=application
orion.audit.prefer-json-column=true
```

`application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/appdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none

orion:
  audit:
    enabled: true
    database-enabled: true
    initialize-schema: true
    table-name: audit_log
    listener-mode: auto
    use-spring-security: true
    capture-request-info: true
    entity-type-format: qualified
    default-source: application
    prefer-json-column: true
```

### H2

`application.properties`

```properties
spring.datasource.url=jdbc:h2:mem:appdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=none
spring.h2.console.enabled=true

orion.audit.enabled=true
orion.audit.database-enabled=true
orion.audit.initialize-schema=true
orion.audit.table-name=audit_log
orion.audit.listener-mode=auto
orion.audit.use-spring-security=true
orion.audit.capture-request-info=true
orion.audit.entity-type-format=simple
orion.audit.default-source=application
orion.audit.prefer-json-column=false
```

`application.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:appdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: none
  h2:
    console:
      enabled: true

orion:
  audit:
    enabled: true
    database-enabled: true
    initialize-schema: true
    table-name: audit_log
    listener-mode: auto
    use-spring-security: true
    capture-request-info: true
    entity-type-format: simple
    default-source: application
    prefer-json-column: false
```

Notes:

- `initialize-schema=true` lets the library create the audit table automatically if it is missing
- if you prefer Flyway-managed audit DDL, set `initialize-schema=false` and use the audit Flyway migration locations instead
- `prefer-json-column=false` is a good default for H2 because it stores JSON-like payloads as CLOB

## Common Configuration Examples

### Change the audit table name

```properties
orion.audit.table-name=audit_logs
```

### Use a schema

```properties
orion.audit.schema=audit
orion.audit.table-name=audit_log
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

### Store simple entity type names

```properties
orion.audit.entity-type-format=simple
```

This stores `User` instead of `com.example.auth.User`.

### Write to database and application logs

```properties
orion.audit.drivers[0]=DATABASE
orion.audit.drivers[1]=LOGGING
```

### Disable automatic table creation

```properties
orion.audit.initialize-schema=false
```

### Configure Flyway vendor override

```properties
orion.audit.flyway.enabled=true
orion.audit.flyway.vendor=postgresql
orion.audit.flyway.append-to-existing=true
```

### Add extra audit Flyway locations

```properties
orion.audit.flyway.locations[0]=classpath:db/migration/common
orion.audit.flyway.locations[1]=classpath:db/migration/custom-audit
```

## Polymorphic Actors

Laravel Auditing uses a morph-style `user_type` and `user_id` approach. In `orion-audit`, the equivalent columns are:

- `actor_type`
- `actor_id`
- `actor_name`

This allows one audit table to represent actions from different actor families such as:

- `users`
- `customers`
- `admins`

If you configure `orion.audit.actors`, the Spring Security resolver matches the authenticated principal and fills those actor columns from configured property paths.

### One actor type

```properties
orion.audit.actors[0].type=users
orion.audit.actors[0].principal-class=com.example.security.UserPrincipal
orion.audit.actors[0].id-property=id
orion.audit.actors[0].name-property=fullName
```

### Multiple actor types

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

### YAML example

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
- `principal-class` is the fully qualified class name of the authenticated Spring Security principal
- `id-property` is the bean property path used for `actor_id`
- `name-property` is the bean property path used for `actor_name`
- `tenant-id-property` is optional and can be used to populate tenant context from the principal
- if no custom tenant resolver is registered, the library also checks request headers, request attributes, security principal fields, and MDC for tenant values
- if you do not configure actors, the library keeps the fallback behavior and uses `authentication.getName()`
- if you configure one actor mapping without `principal-class`, that mapping acts as a default mapping for any authenticated principal

## How Table Creation Works

There are two supported approaches.

### Option 1. Automatic startup creation

This is the default behavior.

If the audit table does not exist, the library creates it automatically on startup.

```properties
orion.audit.initialize-schema=true
```

This is the easiest option if you want the library to work without managing audit DDL yourself.

### Option 2. Flyway-managed creation

The library also ships vendor-specific Flyway migrations.

Locations:

- `classpath:META-INF/orion-audit/db/migration/postgresql`
- `classpath:META-INF/orion-audit/db/migration/mysql`
- `classpath:META-INF/orion-audit/db/migration/h2`

Important:

- if you do not override `spring.flyway.locations`, the library can contribute its migration location automatically
- if you override `spring.flyway.locations`, include the library migration location yourself

Example:

```properties
spring.flyway.locations=classpath:db/migration,classpath:META-INF/orion-audit/db/migration/postgresql
```

## Configuration Reference

### Core properties

| Property | Default | Meaning |
| --- | --- | --- |
| `orion.audit.enabled` | `true` | Master switch for the library |
| `orion.audit.database-enabled` | `true` | Backward-compatible toggle for the built-in database driver |
| `orion.audit.listener-mode` | `AUTO` | `AUTO`, `HIBERNATE`, or `JPA` |
| `orion.audit.table-name` | `audit_log` | Audit table name |
| `orion.audit.schema` | empty | Optional schema name |
| `orion.audit.store-full-snapshot` | `false` | Store full old/new maps instead of changed-only maps |
| `orion.audit.store-empty-changes` | `false` | Persist update records even when no effective changes are detected |
| `orion.audit.fail-on-error` | `false` | Throw audit failures instead of logging only |
| `orion.audit.ignored-fields` | empty | Globally ignored field names |
| `orion.audit.use-spring-security` | `true` | Resolve actor from Spring Security when available |
| `orion.audit.capture-request-info` | `true` | Capture URI, method, IP, user agent, and trace id |
| `orion.audit.entity-type-format` | `QUALIFIED` | Store `entity_type` as the fully qualified class name or the simple class name |
| `orion.audit.default-source` | `application` | Default source label |
| `orion.audit.prefer-json-column` | `true` | Prefer JSON-capable database column types when supported |
| `orion.audit.initialize-schema` | `true` | Create the audit table automatically if missing |
| `orion.audit.ddl-auto` | `none` | Optional Hibernate DDL override hook |

Supported `entity-type-format` values:

- `QUALIFIED`
- `SIMPLE`

### Driver properties

| Property | Default | Meaning |
| --- | --- | --- |
| `orion.audit.drivers` | empty | Explicit built-in drivers to enable |

Supported driver values:

- `DATABASE`
- `LOGGING`

If `drivers` is empty, existing users still get the default database behavior through `database-enabled=true`.

### Actor properties

| Property | Default | Meaning |
| --- | --- | --- |
| `orion.audit.actors` | empty | Optional actor mapping list |
| `orion.audit.actors[].type` | none | Stored value for `actor_type` |
| `orion.audit.actors[].principal-class` | none | Fully qualified authenticated principal class name |
| `orion.audit.actors[].id-property` | `id` | Principal property used for `actor_id` |
| `orion.audit.actors[].name-property` | `name` | Principal property used for `actor_name` |
| `orion.audit.actors[].tenant-id-property` | none | Optional principal property used for tenant id |

### Flyway properties

| Property | Default | Meaning |
| --- | --- | --- |
| `orion.audit.flyway.enabled` | `true` | Enable audit Flyway location contribution |
| `orion.audit.flyway.vendor` | `auto` | Vendor override for audit migrations |
| `orion.audit.flyway.append-to-existing` | `true` | Append audit migration locations to existing Flyway locations |
| `orion.audit.flyway.locations` | empty | Extra audit migration locations |

## Annotations

### `@Audited`

Use it on an entity class.

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

## Stored Audit Fields

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

By default:

- `entity_type` is the fully qualified class name unless you set `orion.audit.entity-type-format=simple`
- `trace_id` is resolved from `traceparent`, `X-B3-TraceId`, `X-Trace-Id`, `X-Request-Id`, request attributes, or MDC
- `tenant_id` is resolved from custom tenant resolvers first, then common request, security, and MDC sources

## Reading Audit Records

Use `AuditQueryService`.

```java
import io.orion.audit.core.model.AuditAction;
import io.orion.audit.core.query.AuditPageRequest;
import io.orion.audit.core.query.AuditQueryCriteria;
import io.orion.audit.core.query.AuditQueryService;

AuditQueryCriteria criteria = AuditQueryCriteria.builder()
    .entityType("com.example.Customer")
    .entityId("42")
    .actorType("users")
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

## Extension Points

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

Run the demo application:

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

Make sure your application uses the latest built version of the library.

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

If your table already exists, no schema change is required because the actor columns already exist.

### tenant_id is always null

The library now tries tenant resolution in this order:

- any custom `TenantResolver` bean
- request headers such as `X-Tenant-Id`
- request attributes such as `tenantId`
- Spring Security principal properties such as `tenantId`
- MDC keys such as `tenantId`

If your application uses a different tenant source, register a custom `TenantResolver`.

### trace_id is always null

The library checks these by default:

- `traceparent`
- `X-B3-TraceId`
- `X-Trace-Id`
- `X-Request-Id`
- request attributes `traceId` and `trace_id`
- MDC keys `traceId` and `trace_id`

If your tracing stack stores the id somewhere else, provide a custom `RequestInfoResolver`.

### I want entity_type to be User instead of the full package name

Set:

```properties
orion.audit.entity-type-format=simple
```

### Audits are not being written

Check:

- your entity is annotated with `@Audited`
- the entity is managed by JPA
- insert, update, or delete is happening through JPA or Hibernate
- auditing is enabled

## Build

```bash
env -u JAVA_HOME mvn clean verify
```

The project targets Java 17+ and Spring Boot 4.x.
