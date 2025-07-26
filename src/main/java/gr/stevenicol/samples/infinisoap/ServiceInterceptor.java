package gr.stevenicol.samples.infinisoap;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.apache.camel.ProducerTemplate;

/**
 * CDI Interceptor that automatically routes annotated methods through the soap-interceptor.
 * This enforces the cross-cutting concerns pattern for all annotated SOAP service methods.
 */
@Intercepted(targetRoute = "", operation = "")
@Interceptor
@Priority(1000)
public class ServiceInterceptor {

    @Inject
    ProducerTemplate producerTemplate;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        
        // Get the annotation from the method
        Intercepted annotation = context.getMethod().getAnnotation(Intercepted.class);
        if (annotation == null) {
            // If method is not annotated, check the class
            annotation = context.getTarget().getClass().getAnnotation(Intercepted.class);
        }
        
        if (annotation != null) {
            // Extract method information
            String operation = annotation.operation().isEmpty() ? 
                context.getMethod().getName() : annotation.operation();
            String targetRoute = annotation.targetRoute();
            
            // Get method parameters (first parameter is typically the request body)
            Object body = context.getParameters().length > 0 ? context.getParameters()[0] : null;
            
            // Create ServiceRequest
            ServiceRequest serviceRequest = new ServiceRequest(operation, body, targetRoute);
            serviceRequest.setUserId("demo-user"); // Get from security context
            serviceRequest.setSessionId("session-" + System.currentTimeMillis());
            serviceRequest.addHeader("operation", operation);
            
            // Determine return type
            Class<?> returnType = context.getMethod().getReturnType();
            
            if (returnType == void.class || returnType == Void.class) {
                // For void methods
                producerTemplate.sendBodyAndHeaders("direct:soap-interceptor", 
                    serviceRequest, serviceRequest.getHeaders());
                return null;
            } else {
                // For methods with return values
                return producerTemplate.requestBodyAndHeaders("direct:soap-interceptor", 
                    serviceRequest, serviceRequest.getHeaders(), returnType);
            }
        }
        
        // If not annotated, proceed normally
        return context.proceed();
    }
}