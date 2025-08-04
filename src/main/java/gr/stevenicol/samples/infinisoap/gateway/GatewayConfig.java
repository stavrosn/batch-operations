package gr.stevenicol.samples.infinisoap.gateway;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Configuration class for Gateway SOAP client settings.
 */
@ApplicationScoped
public class GatewayConfig {

    @ConfigProperty(name = "gateway.soap.endpoint", defaultValue = "http://localhost:8081/gateway/services/cache")
    String endpoint;

    @ConfigProperty(name = "gateway.soap.timeout", defaultValue = "30000")
    int timeoutMs;

    @ConfigProperty(name = "gateway.soap.retry.attempts", defaultValue = "3")
    int retryAttempts;

    @ConfigProperty(name = "gateway.soap.retry.delay", defaultValue = "1000")
    int retryDelayMs;

    @ConfigProperty(name = "gateway.notifications.enabled", defaultValue = "true")
    boolean notificationsEnabled;

    @ConfigProperty(name = "gateway.notifications.async", defaultValue = "true")
    boolean asyncNotifications;

    // Getters
    public String getEndpoint() {
        return endpoint;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public int getRetryDelayMs() {
        return retryDelayMs;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean isAsyncNotifications() {
        return asyncNotifications;
    }

    /**
     * Check if gateway integration is properly configured.
     */
    public boolean isConfigured() {
        return endpoint != null && !endpoint.trim().isEmpty() && notificationsEnabled;
    }

    @Override
    public String toString() {
        return String.format(
            "GatewayConfig{endpoint='%s', timeout=%dms, retries=%d, enabled=%s, async=%s}",
            endpoint, timeoutMs, retryAttempts, notificationsEnabled, asyncNotifications
        );
    }
}