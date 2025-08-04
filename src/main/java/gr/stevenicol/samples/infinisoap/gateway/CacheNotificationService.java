package gr.stevenicol.samples.infinisoap.gateway;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import gr.stevenicol.samples.infinisoap.CacheMetadata;

import java.util.concurrent.CompletableFuture;

/**
 * High-level service for sending cache completion notifications to the gateway.
 * Integrates with existing CacheMetadata objects from the streaming cache service.
 */
@ApplicationScoped
public class CacheNotificationService {

    @Inject
    CacheGatewayClient gatewayClient;

    @Inject 
    GatewayConfig gatewayConfig;

    /**
     * Notify gateway of cache completion using CacheMetadata from streaming service.
     * 
     * @param cacheMetadata Metadata from the streaming cache operation
     * @param dataType Type of data that was cached (e.g., "PERSONS_XML", "JSON")
     * @return CompletableFuture with notification success status
     */
    public CompletableFuture<Boolean> notifyCacheCompletion(CacheMetadata cacheMetadata, String dataType) {
        if (!gatewayConfig.isConfigured()) {
            System.out.println("⚠️ Gateway notifications disabled or not configured - skipping notification");
            return CompletableFuture.completedFuture(false);
        }

        // Generate job ID from cache metadata
        String jobId = CacheGatewayClient.generateJobId(cacheMetadata.getOriginalKey());
        
        System.out.println("🔔 Preparing gateway notification for cache completion:");
        System.out.println("   Job ID: " + jobId);
        System.out.println("   Cache Key: " + cacheMetadata.getOriginalKey());
        System.out.println("   Data Size: " + cacheMetadata.getTotalSize() + " bytes");
        System.out.println("   Total Chunks: " + cacheMetadata.getTotalChunks());
        System.out.println("   Chunk Size: " + cacheMetadata.getChunkSize() + " bytes");
        System.out.println("   Data Type: " + dataType);

        return gatewayClient.notifyCacheCompletion(
            jobId,
            cacheMetadata.getOriginalKey(),
            cacheMetadata.getTotalSize(),
            cacheMetadata.getTotalChunks(),
            cacheMetadata.getChunkSize(),
            dataType
        ).handle((success, throwable) -> {
            if (throwable != null) {
                System.err.println("❌ Gateway notification failed with exception: " + throwable.getMessage());
                return false;
            }
            
            if (success) {
                System.out.println("✅ Gateway notification completed successfully for job: " + jobId);
            } else {
                System.err.println("❌ Gateway notification failed for job: " + jobId);
            }
            
            return success;
        });
    }

    /**
     * Convenience method for notifying with standard data type.
     * 
     * @param cacheMetadata Metadata from the streaming cache operation
     * @return CompletableFuture with notification success status
     */
    public CompletableFuture<Boolean> notifyPersonsDataCached(CacheMetadata cacheMetadata) {
        return notifyCacheCompletion(cacheMetadata, "PERSONS_XML");
    }

    /**
     * Test gateway connectivity.
     * 
     * @return CompletableFuture with connection test result
     */
    public CompletableFuture<Boolean> testGatewayConnection() {
        if (!gatewayConfig.isConfigured()) {
            System.out.println("⚠️ Gateway not configured - connection test skipped");
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            System.out.println("🔍 Testing gateway connection: " + gatewayConfig.getEndpoint());
            boolean result = gatewayClient.testConnection();
            
            if (result) {
                System.out.println("✅ Gateway connection test successful");
            } else {
                System.err.println("❌ Gateway connection test failed");
            }
            
            return result;
        });
    }

    /**
     * Get current gateway configuration status.
     */
    public void logConfigurationStatus() {
        System.out.println("🔧 Gateway Configuration Status:");
        System.out.println("   " + gatewayConfig.toString());
        System.out.println("   Configured: " + gatewayConfig.isConfigured());
    }
}