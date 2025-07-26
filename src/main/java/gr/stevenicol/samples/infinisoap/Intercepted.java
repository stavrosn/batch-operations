package gr.stevenicol.samples.infinisoap;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark SOAP service methods that should be intercepted
 * for auditing and security purposes.
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Intercepted {
    /**
     * The target route to execute after interception
     */
    String targetRoute();
    
    /**
     * The operation name for auditing
     */
    String operation() default "";
}