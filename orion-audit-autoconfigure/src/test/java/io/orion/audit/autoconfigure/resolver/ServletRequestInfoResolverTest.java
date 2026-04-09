package io.orion.audit.autoconfigure.resolver;

import io.orion.audit.core.model.RequestInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class ServletRequestInfoResolverTest {

    private final ServletRequestInfoResolver resolver = new ServletRequestInfoResolver("application");

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void shouldExtractTraceIdFromTraceParentHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/1");
        request.addHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestInfo requestInfo = resolver.resolve();

        assertThat(requestInfo.getTraceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void shouldFallbackToMdcTraceId() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put("traceId", "trace-mdc");

        RequestInfo requestInfo = resolver.resolve();

        assertThat(requestInfo.getTraceId()).isEqualTo("trace-mdc");
    }
}
