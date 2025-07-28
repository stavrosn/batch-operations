package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing large string data in Infinispan cache.
 * Optimized for handling 125-150MB string values.
 */
@ApplicationScoped
public class LargeStringCacheService {

    private static final Logger log = LoggerFactory.getLogger(LargeStringCacheService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Commented out to avoid conflicts with Camel Infinispan component
    // Use CamelInfinispanCacheService instead
    /*
    @Inject
    RemoteCacheManager cacheManager;
    
    private RemoteCache<String, CacheData> cache;

    @PostConstruct
    void initCache() {
        cache = cacheManager.getCache("large-string-cache");
    }
    */

    // All methods commented out - use CamelInfinispanCacheService instead
    /*
    /**
     * Store large string data in cache with current timestamp
     */
//    public void putLargeString(String key, String data) {
//        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
//        CacheData cacheData = new CacheData(timestamp, data);
//
//        log.info("Storing large string in cache: key={}, dataSize={} characters, timestamp={}",
//                 key, data.length(), timestamp);
//
//        cache.put(key, cacheData);
//
//        log.info("Successfully stored large string in cache: key={}", key);
//    }

    /**
     * Store large string data asynchronously
     */
//    public CompletableFuture<Void> putLargeStringAsync(String key, String data) {
//        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
//        CacheData cacheData = new CacheData(timestamp, data);
//
//        log.info("Storing large string in cache asynchronously: key={}, dataSize={} characters",
//                 key, data.length());
//
//        return cache.putAsync(key, cacheData)
//                   .thenRun(() -> log.info("Successfully stored large string in cache: key={}", key));
//    }

    /**
     * Retrieve large string data from cache
     */
//    public CacheData getLargeString(String key) {
//        log.info("Retrieving large string from cache: key={}", key);
//
//        CacheData result = cache.get(key);
//
//        if (result != null) {
//            log.info("Found cached data: key={}, dataSize={} characters, timestamp={}",
//                     key, result.getData().length(), result.getDateString());
//        } else {
//            log.info("No cached data found for key={}", key);
//        }
//
//        return result;
//    }

    /**
     * Retrieve large string data asynchronously
     */
//    public CompletableFuture<CacheData> getLargeStringAsync(String key) {
//        log.info("Retrieving large string from cache asynchronously: key={}", key);
//
//        return cache.getAsync(key)
//                   .thenApply(result -> {
//                       if (result != null) {
//                           log.info("Found cached data: key={}, dataSize={} characters, timestamp={}",
//                                    key, result.getData().length(), result.getDateString());
//                       } else {
//                           log.info("No cached data found for key={}", key);
//                       }
//                       return result;
//                   });
//    }

    /**
     * Check if key exists in cache
     */
//    public boolean containsKey(String key) {
//        return cache.containsKey(key);
//    }

    /**
     * Remove entry from cache
     */
//    public void remove(String key) {
//        log.info("Removing entry from cache: key={}", key);
//        cache.remove(key);
//    }

    /**
     * Get cache statistics
     */
//    public void printCacheStats() {
//        try {
//            log.info("Cache stats: size={}, hits={}, misses={}",
//                     cache.size(),
//                     cache.clientStatistics().getRemoteHits(),
//                     cache.clientStatistics().getRemoteMisses());
//        } catch (Exception e) {
//            log.warn("Unable to retrieve cache statistics: {}", e.getMessage());
//        }
//    }

}