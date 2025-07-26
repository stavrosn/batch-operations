package gr.stevenicol.samples.infinisoap;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

public class ServiceRequest {
    private String operation;
    private Object body;
    private String targetRoute;
    private LocalDateTime timestamp;
    private String sessionId;
    private String userId;
    private Map<String, Object> headers;
    private Map<String, Object> auditData;

    public ServiceRequest() {
        this.timestamp = LocalDateTime.now();
        this.headers = new HashMap<>();
        this.auditData = new HashMap<>();
    }

    public ServiceRequest(String operation, Object body, String targetRoute) {
        this();
        this.operation = operation;
        this.body = body;
        this.targetRoute = targetRoute;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public String getTargetRoute() {
        return targetRoute;
    }

    public void setTargetRoute(String targetRoute) {
        this.targetRoute = targetRoute;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public void addHeader(String key, Object value) {
        this.headers.put(key, value);
    }

    public Map<String, Object> getAuditData() {
        return auditData;
    }

    public void setAuditData(Map<String, Object> auditData) {
        this.auditData = auditData;
    }

    public void addAuditData(String key, Object value) {
        this.auditData.put(key, value);
    }

    @Override
    public String toString() {
        return "ServiceRequest{" +
                "operation='" + operation + '\'' +
                ", targetRoute='" + targetRoute + '\'' +
                ", timestamp=" + timestamp +
                ", sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}