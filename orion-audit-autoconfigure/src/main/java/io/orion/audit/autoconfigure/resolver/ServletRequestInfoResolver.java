package io.orion.audit.autoconfigure.resolver;

import io.orion.audit.core.model.RequestInfo;
import io.orion.audit.core.spi.RequestInfoResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves request metadata from the current servlet request.
 */
public class ServletRequestInfoResolver implements RequestInfoResolver {

    private final String defaultSource;

    public ServletRequestInfoResolver(String defaultSource) {
        this.defaultSource = defaultSource;
    }

    @Override
    public RequestInfo resolve() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return RequestInfo.empty(defaultSource);
        }

        HttpServletRequest request = servletAttributes.getRequest();
        return new RequestInfo(
            resolveClientIp(request),
            request.getHeader("User-Agent"),
            request.getRequestURI(),
            request.getMethod(),
            resolveTraceId(request),
            "http"
        );
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveTraceId(HttpServletRequest request) {
        for (String header : new String[]{"traceparent", "X-B3-TraceId", "X-Trace-Id"}) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
