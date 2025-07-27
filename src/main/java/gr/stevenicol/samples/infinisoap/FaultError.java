package gr.stevenicol.samples.infinisoap;

/**
 * Custom exception for interceptor-level errors (audit and security failures).
 * This exception can be thrown from audit-incoming or security-check routes
 * to halt processing and return an error response to the SOAP client.
 */
public class FaultError extends RuntimeException {

    private final String errorCode;
    private final String errorType;

    public FaultError(String message) {
        super(message);
        this.errorCode = "GENERAL_ERROR";
        this.errorType = "INTERCEPTOR_ERROR";
    }

    public FaultError(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = "INTERCEPTOR_ERROR";
    }

    public FaultError(String message, String errorCode, String errorType) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = errorType;
    }

    public FaultError(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENERAL_ERROR";
        this.errorType = "INTERCEPTOR_ERROR";
    }

    public FaultError(String message, String errorCode, String errorType, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorType = errorType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorType() {
        return errorType;
    }

    @Override
    public String toString() {
        return "FaultError{" +
                "errorType='" + errorType + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }

    // Static factory methods for common error scenarios
    public static FaultError auditError(String message) {
        return new FaultError(message, "AUDIT_ERROR", "AUDIT_FAILURE");
    }

    public static FaultError securityError(String message) {
        return new FaultError(message, "SECURITY_ERROR", "SECURITY_FAILURE");
    }

    public static FaultError authenticationError(String message) {
        return new FaultError(message, "AUTH_ERROR", "AUTHENTICATION_FAILURE");
    }

    public static FaultError authorizationError(String message) {
        return new FaultError(message, "AUTHZ_ERROR", "AUTHORIZATION_FAILURE");
    }

    public static FaultError validationError(String message) {
        return new FaultError(message, "VALIDATION_ERROR", "VALIDATION_FAILURE");
    }
}