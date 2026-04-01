package io.orion.audit.core.model;

import java.util.Objects;

/**
 * Request metadata captured when an audited action is triggered in an HTTP flow.
 */
public final class RequestInfo {

    private final String ipAddress;
    private final String userAgent;
    private final String requestUri;
    private final String httpMethod;
    private final String traceId;
    private final String source;

    public RequestInfo(
        String ipAddress,
        String userAgent,
        String requestUri,
        String httpMethod,
        String traceId,
        String source
    ) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.requestUri = requestUri;
        this.httpMethod = httpMethod;
        this.traceId = traceId;
        this.source = source;
    }

    public static RequestInfo empty(String source) {
        return new RequestInfo(null, null, null, null, null, source);
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RequestInfo that)) {
            return false;
        }
        return Objects.equals(ipAddress, that.ipAddress)
            && Objects.equals(userAgent, that.userAgent)
            && Objects.equals(requestUri, that.requestUri)
            && Objects.equals(httpMethod, that.httpMethod)
            && Objects.equals(traceId, that.traceId)
            && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, userAgent, requestUri, httpMethod, traceId, source);
    }
}
