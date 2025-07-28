package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Camel-based service for managing large string data in Infinispan cache.
 * Uses Camel routes for cache operations, optimized for 125-150MB string values.
 */
@ApplicationScoped
public class CamelInfinispanCacheService {

    private static final Logger log = LoggerFactory.getLogger(CamelInfinispanCacheService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Inject
    ProducerTemplate producerTemplate;

    /**
     * Store large string data in cache with current timestamp
     * Returns true if successful, false if failed
     */
    public boolean putLargeString(String key, String data) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        CacheData cacheData = new CacheData(timestamp, data);
        
        log.info("Storing large string in cache via Camel: key={}, dataSize={} characters, timestamp={}", 
                 key, data.length(), timestamp);
        
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("CamelInfinispanOperation", "PUT");
            headers.put("CamelInfinispanKey", key);
            
            // Use requestBodyAndHeaders to get response and detect errors
            Object result = producerTemplate.requestBodyAndHeaders("direct:cache-put", cacheData, headers);
            
            log.info("Successfully stored large string in cache: key={}", key);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to store large string in cache: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Store large string data asynchronously
     * Returns CompletableFuture<Boolean> - true if successful, false if failed
     */
    public CompletableFuture<Boolean> putLargeStringAsync(String key, String data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return putLargeString(key, data);
            } catch (Exception e) {
                log.error("Async cache operation failed for key={}: {}", key, e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Retrieve large string data from cache
     */
    public String getLargeString(String key) {
        CacheData result = getLargeStringData(key);
        return result != null ? result.getData() : null;
    }

    /**
     * Retrieve large string data from cache as CacheData object
     */
    public CacheData getLargeStringData(String key) {
        log.info("Retrieving large string from cache via Camel: key={}", key);
        
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelInfinispanOperation", "GET");
        headers.put("CamelInfinispanKey", key);
        
        CacheData result = producerTemplate.requestBodyAndHeaders("direct:cache-get", null, headers, CacheData.class);
        
        if (result != null) {
            log.info("Found cached data: key={}, dataSize={} characters, timestamp={}", 
                     key, result.getData().length(), result.getDateString());
        } else {
            log.info("No cached data found for key={}", key);
        }
        
        return result;
    }

    /**
     * Retrieve large string data asynchronously
     */
    public CompletableFuture<String> getLargeStringAsync(String key) {
        return CompletableFuture.supplyAsync(() -> getLargeString(key));
    }

    /**
     * Retrieve large string data asynchronously as CacheData
     */
    public CompletableFuture<CacheData> getLargeStringDataAsync(String key) {
        return CompletableFuture.supplyAsync(() -> getLargeStringData(key));
    }

    /**
     * Check if key exists in cache
     */
    public boolean containsKey(String key) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelInfinispanOperation", "CONTAINSKEY");
        headers.put("CamelInfinispanKey", key);
        
        Boolean result = producerTemplate.requestBodyAndHeaders("direct:cache-containskey", null, headers, Boolean.class);
        return result != null ? result : false;
    }

    /**
     * Remove entry from cache
     * Returns true if successful, false if failed
     */
    public boolean remove(String key) {
        log.info("Removing entry from cache via Camel: key={}", key);
        
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("CamelInfinispanOperation", "REMOVE");
            headers.put("CamelInfinispanKey", key);
            
            Object result = producerTemplate.requestBodyAndHeaders("direct:cache-remove", null, headers);
            log.info("Successfully removed entry from cache: key={}", key);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to remove entry from cache: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get cache size
     */
    public int getCacheSize() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelInfinispanOperation", "SIZE");
        
        Integer result = producerTemplate.requestBodyAndHeaders("direct:cache-size", null, headers, Integer.class);
        return result != null ? result : 0;
    }

    /**
     * Clear entire cache
     */
    public void clearCache() {
        log.info("Clearing entire cache via Camel");
        
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelInfinispanOperation", "CLEAR");
        
        producerTemplate.sendBodyAndHeaders("direct:cache-clear", null, headers);
    }

    /**
     * Enhanced async operation with detailed callback
     */
    public CompletableFuture<Boolean> putLargeStringAsyncWithCallback(String key, String data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean success = putLargeString(key, data);
                
                if (success) {
                    // Verify the data was actually stored by reading it back
                    CacheData verification = getLargeStringData(key);
                    if (verification != null && verification.getData().equals(data)) {
                        log.info("ASYNC-VERIFY: ✅ Data verified successfully stored for key={}", key);
                        return true;
                    } else {
                        log.error("ASYNC-VERIFY: ❌ Data verification failed for key={}", key);
                        return false;
                    }
                } else {
                    return false;
                }
                
            } catch (Exception e) {
                log.error("ASYNC-CALLBACK: ❌ Operation failed for key={}: {}", key, e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Async operation with progress callback
     */
    public CompletableFuture<Boolean> putLargeStringAsyncWithProgress(String key, String data, 
                                                                     java.util.function.Consumer<String> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                progressCallback.accept("Starting cache operation for key: " + key);
                
                boolean success = putLargeString(key, data);
                
                if (success) {
                    progressCallback.accept("✅ Cache operation completed successfully for key: " + key);
                    
                    // Optional verification
                    progressCallback.accept("Verifying stored data...");
                    CacheData verification = getLargeStringData(key);
                    if (verification != null) {
                        progressCallback.accept("✅ Data verification successful for key: " + key);
                        return true;
                    } else {
                        progressCallback.accept("❌ Data verification failed for key: " + key);
                        return false;
                    }
                } else {
                    progressCallback.accept("❌ Cache operation failed for key: " + key);
                    return false;
                }
                
            } catch (Exception e) {
                progressCallback.accept("❌ Exception during cache operation: " + e.getMessage());
                return false;
            }
        });
    }
}