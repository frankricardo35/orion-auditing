package io.orion.audit.autoconfigure.resolver;

import io.orion.audit.core.model.TenantContext;
import io.orion.audit.core.spi.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Default tenant resolver that checks common request, security, and MDC sources.
 */
public class DefaultTenantResolver implements TenantResolver {

    @Override
    public TenantContext resolveTenant() {
        String tenantId = resolveFromRequest();
        if (!hasText(tenantId)) {
            tenantId = resolveFromSecurityContext();
        }
        if (!hasText(tenantId)) {
            tenantId = resolveFromMdc();
        }
        return hasText(tenantId) ? TenantContext.of(tenantId) : null;
    }

    private String resolveFromRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        for (String header : new String[]{"X-Tenant-Id", "Tenant-Id", "X-TenantId", "tenant-id"}) {
            String value = request.getHeader(header);
            if (hasText(value)) {
                return value;
            }
        }
        for (String attribute : new String[]{"tenantId", "tenant_id"}) {
            Object value = request.getAttribute(attribute);
            if (value != null && hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private String resolveFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return null;
        }
        BeanWrapperImpl wrapper = new BeanWrapperImpl(principal);
        for (String property : new String[]{"tenantId", "tenant.id"}) {
            try {
                Object value = wrapper.getPropertyValue(property);
                if (value != null && hasText(String.valueOf(value))) {
                    return String.valueOf(value);
                }
            }
            catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private String resolveFromMdc() {
        for (String key : new String[]{"tenantId", "tenant_id"}) {
            String value = MDC.get(key);
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
