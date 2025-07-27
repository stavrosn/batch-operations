package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.cxf.binding.soap.SoapFault;
import javax.xml.namespace.QName;

@ApplicationScoped
public class InterceptorRouter extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Exception handling for FaultError - convert to SoapFault but don't handle
        onException(FaultError.class)
                .handled(false)  // Let it propagate to CXF
                .process(exchange -> {
                    FaultError faultError = exchange.getProperty(org.apache.camel.Exchange.EXCEPTION_CAUGHT, FaultError.class);
                    
                    log.info("DEBUG: Caught FaultError: " + faultError.toString());
                    
                    // Create custom SOAP fault
                    SoapFault soapFault = new SoapFault(
                        faultError.getMessage(),
                        new QName("http://gr.stevenicol.samples/soap/SampleService", faultError.getErrorCode())
                    );
                    
                    // Set custom fault code based on error type
                    switch (faultError.getErrorType()) {
                        case "AUTHENTICATION_FAILURE":
                            soapFault.setFaultCode(new QName("http://schemas.xmlsoap.org/soap/envelope/", "Client.Authentication"));
                            break;
                        case "AUTHORIZATION_FAILURE":
                            soapFault.setFaultCode(new QName("http://schemas.xmlsoap.org/soap/envelope/", "Client.Authorization"));
                            break;
                        case "VALIDATION_FAILURE":
                            soapFault.setFaultCode(new QName("http://schemas.xmlsoap.org/soap/envelope/", "Client.Validation"));
                            break;
                        case "AUDIT_FAILURE":
                            soapFault.setFaultCode(new QName("http://schemas.xmlsoap.org/soap/envelope/", "Server.Audit"));
                            break;
                        default:
                            soapFault.setFaultCode(new QName("http://schemas.xmlsoap.org/soap/envelope/", "Server"));
                            break;
                    }
                    
                    log.info("DEBUG: Created SoapFault with code: " + soapFault.getFaultCode() + ", message: " + soapFault.getMessage());
                    
                    // Replace the original exception with SoapFault
                    exchange.setProperty(org.apache.camel.Exchange.EXCEPTION_CAUGHT, soapFault);
                });

        // Centralized interceptor route for cross-cutting concerns
        from("direct:soap-interceptor")
                .log("=== INTERCEPTOR: Processing ${header.operation} operation ===")
                .to("direct:audit-incoming")
                .to("direct:security-check")
                .process(exchange -> {
                    ServiceRequest serviceRequest = exchange.getIn().getBody(ServiceRequest.class);
                    if (serviceRequest != null) {
                        // Store original ServiceRequest for audit purposes
                        exchange.setProperty("originalServiceRequest", serviceRequest);
                        // Set the original body for the target route
                        exchange.getIn().setBody(serviceRequest.getBody());
                        // Copy any additional headers from ServiceRequest
                        serviceRequest.getHeaders().forEach((key, value) -> 
                            exchange.getIn().setHeader(key, value));
                        
                        // IMPORTANT: Set the targetRoute as a header for toD to use
                        exchange.getIn().setHeader("targetRoute", serviceRequest.getTargetRoute());
                        
                        // Debug logging
                        log.info("DEBUG: About to call target route: " + serviceRequest.getTargetRoute());
                        log.info("DEBUG: Request body for target route: " + serviceRequest.getBody());
                        log.info("DEBUG: targetRoute header set to: " + exchange.getIn().getHeader("targetRoute"));
                    }
                })
                .toD("${header.targetRoute}")  // Use toD instead of recipientList to capture response
                .process(exchange -> {
                    // Debug: Check what we got back
                    Object response = exchange.getIn().getBody();
                    log.info("DEBUG: Response received from target route: " + response);
                    log.info("DEBUG: Response class: " + (response != null ? response.getClass().getName() : "null"));
                    
                    // Store the response for audit logging
                    exchange.setProperty("serviceResponse", response);
                    
                    // Also set operation header for audit-outgoing
                    ServiceRequest originalRequest = (ServiceRequest) exchange.getProperty("originalServiceRequest");
                    if (originalRequest != null) {
                        exchange.getIn().setHeader("operation", originalRequest.getOperation());
                    }
                })
                .to("direct:audit-outgoing");

        // Audit incoming requests
        from("direct:audit-incoming")
                .log("AUDIT-IN: Operation=${header.operation}, User=${header.userId}, Session=${header.sessionId}, Timestamp=${date:now:yyyy-MM-dd HH:mm:ss}")
                .process(exchange -> {
                    ServiceRequest serviceRequest = exchange.getIn().getBody(ServiceRequest.class);
                    if (serviceRequest != null) {
                        // Example audit validation - check for required audit fields
                        String operation = serviceRequest.getOperation();
                        String userId = serviceRequest.getUserId();
                        String sessionId = serviceRequest.getSessionId();
                        
                        // Example: Block certain operations during maintenance window
                        if ("deletePerson".equals(operation)) {
                            log.warn("AUDIT BLOCK: deletePerson operation is disabled during maintenance");
                            throw FaultError.auditError("Service temporarily unavailable - deletePerson operation is disabled during maintenance window");
                        }
                        
                        // Example: Validate audit requirements
                        if (sessionId == null || sessionId.isEmpty()) {
                            log.warn("AUDIT VALIDATION FAILED: Missing session ID for operation: " + operation);
                            throw FaultError.auditError("Invalid session - session ID is required for all operations");
                        }
                        
                        serviceRequest.addAuditData("audit_start", System.currentTimeMillis());
                        serviceRequest.addAuditData("client_ip", "127.0.0.1"); // In real scenario, get from request
                        
                        // Store audit_start in exchange property for later use
                        exchange.setProperty("audit_start", System.currentTimeMillis());
                    }
                });

        // Security checks
        from("direct:security-check")
                .log("SECURITY: Validating access for operation=${header.operation}")
                .process(exchange -> {
                    ServiceRequest serviceRequest = exchange.getIn().getBody(ServiceRequest.class);
                    if (serviceRequest != null) {
                        // Basic security validation
                        String operation = serviceRequest.getOperation();
                        String userId = serviceRequest.getUserId();
                        String sessionId = serviceRequest.getSessionId();
                        
                        // Example: Authentication check
                        if (userId == null || userId.isEmpty()) {
                            log.warn("SECURITY ERROR: Missing user authentication for operation: " + operation);
                            throw FaultError.authenticationError("Authentication required - user ID is missing");
                        }
                        
                        // Example: Authorization check for sensitive operations
                        if ("deletePerson".equals(operation) && !"admin-user".equals(userId)) {
                            log.warn("SECURITY ERROR: User '" + userId + "' not authorized for operation: " + operation);
                            throw FaultError.authorizationError("Access denied - insufficient privileges for deletePerson operation");
                        }
                        
                        // Example: Session validation
                        if (sessionId != null && sessionId.startsWith("expired-")) {
                            log.warn("SECURITY ERROR: Expired session for user: " + userId);
                            throw FaultError.authenticationError("Session expired - please re-authenticate");
                        }
                        
                        // Example: Rate limiting check (simplified)
                        if ("getPersons".equals(operation) && sessionId != null && sessionId.contains("rate-limited")) {
                            log.warn("SECURITY ERROR: Rate limit exceeded for user: " + userId);
                            throw FaultError.validationError("Rate limit exceeded - too many requests");
                        }
                        
                        serviceRequest.addAuditData("security_status", "authorized");
                        log.info("SECURITY: User '" + userId + "' authorized for operation: " + operation);
                    }
                });

        // Audit outgoing responses
        from("direct:audit-outgoing")
                .process(exchange -> {
                    // Get the stored response for logging
                    Object response = exchange.getProperty("serviceResponse");
                    String operation = exchange.getIn().getHeader("operation", String.class);
                    
                    if (response != null) {
                        log.info("AUDIT-OUT: Operation '" + operation + "' completed successfully, Response: " + response.toString());
                    } else {
                        log.info("AUDIT-OUT: Operation '" + operation + "' completed successfully, Response: [empty/void]");
                    }
                    
                    long endTime = System.currentTimeMillis();
                    // Calculate processing time if audit_start was set
                    Object startTime = exchange.getProperty("audit_start");
                    if (startTime != null) {
                        long processingTime = endTime - (Long) startTime;
                        log.info("AUDIT: Processing time for operation '" + operation + "': " + processingTime + "ms");
                    }
                });
    }
}