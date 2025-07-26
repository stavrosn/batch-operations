package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class InterceptorRouter extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Centralized interceptor route for cross-cutting concerns
        from("direct:soap-interceptor")
                .log("=== INTERCEPTOR: Processing ${header.operation} operation ===")
                .to("direct:audit-incoming")
                .to("direct:security-check")
                .process(exchange -> {
                    ServiceRequest serviceRequest = exchange.getIn().getBody(ServiceRequest.class);
                    if (serviceRequest != null) {
                        // Set the original body for the target route
                        exchange.getIn().setBody(serviceRequest.getBody());
                        // Copy any additional headers from ServiceRequest
                        serviceRequest.getHeaders().forEach((key, value) -> 
                            exchange.getIn().setHeader(key, value));
                    }
                })
                .recipientList(simple("${header.targetRoute}"))
                .to("direct:audit-outgoing");

        // Audit incoming requests
        from("direct:audit-incoming")
                .log("AUDIT-IN: Operation=${header.operation}, User=${header.userId}, Session=${header.sessionId}, Timestamp=${date:now:yyyy-MM-dd HH:mm:ss}")
                .process(exchange -> {
                    ServiceRequest serviceRequest = exchange.getIn().getBody(ServiceRequest.class);
                    if (serviceRequest != null) {
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
                        
                        // Example: Check if user is authorized for this operation
                        if ("getPersons".equals(operation) && (userId == null || userId.isEmpty())) {
                            log.warn("SECURITY WARNING: Unauthorized access attempt for operation: " + operation);
                            serviceRequest.addAuditData("security_warning", "Missing user authentication");
                        } else {
                            serviceRequest.addAuditData("security_status", "authorized");
                        }
                    }
                });

        // Audit outgoing responses
        from("direct:audit-outgoing")
                .log("AUDIT-OUT: Operation completed successfully, Response=${body}")
                .process(exchange -> {
                    long endTime = System.currentTimeMillis();
                    // Calculate processing time if audit_start was set
                    Object startTime = exchange.getProperty("audit_start");
                    if (startTime != null) {
                        long processingTime = endTime - (Long) startTime;
                        log.info("AUDIT: Processing time for operation: " + processingTime + "ms");
                    }
                });
    }
}