package io.orion.audit.autoconfigure.resolver;

import io.orion.audit.core.model.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTenantResolverTest {

    private final DefaultTenantResolver resolver = new DefaultTenantResolver();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void shouldResolveTenantFromRequestHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-request");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        TenantContext tenantContext = resolver.resolveTenant();

        assertThat(tenantContext).isNotNull();
        assertThat(tenantContext.getTenantId()).isEqualTo("tenant-request");
    }

    @Test
    void shouldResolveTenantFromSecurityPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(new TenantPrincipal("tenant-security"), "n/a", List.of())
        );

        TenantContext tenantContext = resolver.resolveTenant();

        assertThat(tenantContext).isNotNull();
        assertThat(tenantContext.getTenantId()).isEqualTo("tenant-security");
    }

    @Test
    void shouldResolveTenantFromMdc() {
        MDC.put("tenantId", "tenant-mdc");

        TenantContext tenantContext = resolver.resolveTenant();

        assertThat(tenantContext).isNotNull();
        assertThat(tenantContext.getTenantId()).isEqualTo("tenant-mdc");
    }

    static final class TenantPrincipal {
        private final String tenantId;

        TenantPrincipal(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getTenantId() {
            return tenantId;
        }
    }
}
