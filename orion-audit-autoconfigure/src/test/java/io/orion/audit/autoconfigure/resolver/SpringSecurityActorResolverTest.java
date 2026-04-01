package io.orion.audit.autoconfigure.resolver;

import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.core.model.ActorInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringSecurityActorResolverTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldResolveConfiguredActorMapping() {
        AuditProperties.Actor userActor = new AuditProperties.Actor();
        userActor.setType("users");
        userActor.setPrincipalClass(UserPrincipal.class.getName());
        userActor.setIdProperty("id");
        userActor.setNameProperty("displayName");
        userActor.setTenantIdProperty("tenantId");

        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal("42", "Alice Admin", "tenant-a"), "n/a", List.of())
        );

        ActorInfo actor = new SpringSecurityActorResolver(List.of(userActor)).resolve();

        assertThat(actor.getActorId()).isEqualTo("42");
        assertThat(actor.getActorName()).isEqualTo("Alice Admin");
        assertThat(actor.getActorType()).isEqualTo("users");
        assertThat(actor.getTenantId()).isEqualTo("tenant-a");
    }

    @Test
    void shouldUseFallbackMappingWithoutPrincipalClass() {
        AuditProperties.Actor fallbackActor = new AuditProperties.Actor();
        fallbackActor.setType("customers");
        fallbackActor.setIdProperty("customerId");
        fallbackActor.setNameProperty("fullName");

        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(new CustomerPrincipal("cust-7", "Bob Buyer"), "n/a", List.of())
        );

        ActorInfo actor = new SpringSecurityActorResolver(List.of(fallbackActor)).resolve();

        assertThat(actor.getActorId()).isEqualTo("cust-7");
        assertThat(actor.getActorName()).isEqualTo("Bob Buyer");
        assertThat(actor.getActorType()).isEqualTo("customers");
    }

    @Test
    void shouldFallbackToAuthenticationNameWhenNoMappingMatches() {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("plain-user", "n/a", List.of())
        );

        ActorInfo actor = new SpringSecurityActorResolver(List.of()).resolve();

        assertThat(actor.getActorId()).isEqualTo("plain-user");
        assertThat(actor.getActorName()).isEqualTo("plain-user");
        assertThat(actor.getActorType()).isEqualTo("String");
    }

    static final class UserPrincipal {
        private final String id;
        private final String displayName;
        private final String tenantId;

        UserPrincipal(String id, String displayName, String tenantId) {
            this.id = id;
            this.displayName = displayName;
            this.tenantId = tenantId;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTenantId() {
            return tenantId;
        }
    }

    static final class CustomerPrincipal {
        private final String customerId;
        private final String fullName;

        CustomerPrincipal(String customerId, String fullName) {
            this.customerId = customerId;
            this.fullName = fullName;
        }

        public String getCustomerId() {
            return customerId;
        }

        public String getFullName() {
            return fullName;
        }
    }
}
