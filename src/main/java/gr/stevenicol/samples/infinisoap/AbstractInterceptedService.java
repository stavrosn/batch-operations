package gr.stevenicol.samples.infinisoap;

import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;

/**
 * Abstract base class that enforces interceptor usage for all SOAP service methods.
 * All SOAP service implementations should extend this class.
 */
public abstract class AbstractInterceptedService {

    @Inject
    protected ProducerTemplate producerTemplate;

    /**
     * Enforced method to route all service calls through the interceptor.
     * This ensures consistent auditing, security, and cross-cutting concerns.
     */
    protected final <T> T executeWithInterceptor(String operation, Object body, String targetRoute, Class<T> responseType) {
        ServiceRequest serviceRequest = new ServiceRequest(operation, body, targetRoute);
        
        // Set user context (in real scenario, get from security context)
        serviceRequest.setUserId(getCurrentUserId());
        serviceRequest.setSessionId(getCurrentSessionId());
        serviceRequest.addHeader("operation", operation);
        
        if (responseType == Void.class || responseType == void.class) {
            // For void operations
            producerTemplate.sendBodyAndHeaders("direct:soap-interceptor", 
                serviceRequest, serviceRequest.getHeaders());
            return null;
        } else {
            // For operations with return values
            return producerTemplate.requestBodyAndHeaders("direct:soap-interceptor", 
                serviceRequest, serviceRequest.getHeaders(), responseType);
        }
    }

    /**
     * Convenience method for void operations
     */
    protected final void executeWithInterceptor(String operation, Object body, String targetRoute) {
        executeWithInterceptor(operation, body, targetRoute, Void.class);
    }

    /**
     * Override this method to provide user context from security framework
     */
    protected String getCurrentUserId() {
        return "demo-user"; // In real scenario: SecurityContextHolder.getContext().getAuthentication().getName()
    }

    /**
     * Override this method to provide session context
     */
    protected String getCurrentSessionId() {
        return "session-" + System.currentTimeMillis();
    }
}