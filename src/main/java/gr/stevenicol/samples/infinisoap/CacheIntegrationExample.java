package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Example integration showing how to use the Camel Infinispan cache 
 * within your SOAP service methods or interceptor routes.
 */
@ApplicationScoped
public class CacheIntegrationExample {

    @Inject
    CamelInfinispanCacheService cacheService;

    /**
     * Example: Cache SOAP response data
     * This could be called from your SampleServiceImpl methods
     */
    public void cacheServiceResponse(String operationName, String userId, Object responseData) {
        // Create cache key based on operation and user
        String cacheKey = String.format("soap-response-%s-%s", operationName, userId);
        
        // Convert response to string (you might want to use JSON serialization)
        String responseString = responseData.toString();
        
        // Cache the response
        cacheService.putLargeString(cacheKey, responseString);
    }

    /**
     * Example: Retrieve cached SOAP response
     */
    public String getCachedServiceResponse(String operationName, String userId) {
        String cacheKey = String.format("soap-response-%s-%s", operationName, userId);
        
        byte[] cachedData = cacheService.getLargeString(cacheKey);
        return new String(cachedData);
    }

    /**
     * Example: Cache large XML/JSON payloads from SOAP operations
     */
    public void cacheLargePayload(String payloadId, String xmlOrJsonData) {
        // Perfect for your 125-150MB requirement
        cacheService.putLargeString(payloadId, xmlOrJsonData);
    }

    /**
     * Example: Integration with your existing interceptor pattern
     * This could be added to your InterceptorRouter
     */
    public void addCachingToInterceptor(String operation, String userId, String sessionId, Object requestData) {
        // Cache the request for audit purposes
        String auditCacheKey = String.format("audit-request-%s-%s-%s", operation, userId, sessionId);
        String requestString = requestData != null ? requestData.toString() : "";
        
        cacheService.putLargeString(auditCacheKey, requestString);
        
        // Check if we have a cached response for this request
        String responseCacheKey = String.format("cached-response-%s-%s", operation, userId);
        byte[] cachedResponse = cacheService.getLargeString(responseCacheKey);
        
        if (cachedResponse != null) {
            // Get full cache data if we need metadata
            CacheData fullCacheData = cacheService.getLargeStringData(responseCacheKey);
            System.out.println("Found cached response from: " + (fullCacheData != null ? fullCacheData.getDateString() : "unknown"));
            // Use cached response instead of processing the request
        }
    }
}