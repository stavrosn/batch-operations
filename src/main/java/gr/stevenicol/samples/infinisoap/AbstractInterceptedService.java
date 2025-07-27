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
        
        try {
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
        } catch (org.apache.camel.CamelExecutionException e) {
            // Check if the root cause is a SoapFault and rethrow it
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof org.apache.cxf.binding.soap.SoapFault) {
                    throw (org.apache.cxf.binding.soap.SoapFault) cause;
                }
                if (cause instanceof FaultError) {
                    FaultError faultError = (FaultError) cause;
                    // Create and throw SoapFault directly
                    org.apache.cxf.binding.soap.SoapFault soapFault = new org.apache.cxf.binding.soap.SoapFault(
                        faultError.getMessage(),
                        new javax.xml.namespace.QName("http://gr.stevenicol.samples/soap/SampleService", faultError.getErrorCode())
                    );
                    throw soapFault;
                }
                cause = cause.getCause();
            }
            // If no SoapFault found, rethrow original exception
            throw e;
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