package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.CompletableFuture;

/**
 * Example showing how to use the streaming cache service for large data.
 * Perfect for 125-150MB data with progress tracking.
 */
@ApplicationScoped
public class StreamingExample {

    @Inject
    StreamingCacheService streamingCacheService;

    /**
     * Example: Upload large data with progress tracking
     */
    public void uploadLargeDataExample() {
        // Simulate 125MB data
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 125 * 1024; i++) { // 125MB (approximately)
            largeData.append("This is test data line ").append(i).append(" with some content to make it larger.\n");
        }

        String data = largeData.toString();
        System.out.println("Generated data size: " + data.length() + " characters");

        // Upload with progress tracking
        CompletableFuture<Boolean> uploadFuture = streamingCacheService.putLargeDataStream(
            "large-document-1", 
            data,
            progress -> {
                System.out.println("📤 UPLOAD: " + progress);
                if (progress.hasError()) {
                    System.err.println("❌ Upload error: " + progress.getMessage());
                } else if (progress.isCompleted()) {
                    System.out.println("✅ Upload completed successfully!");
                }
            }
        );

        // Handle completion
        uploadFuture.thenAccept(success -> {
            if (success) {
                System.out.println("🎉 Large data upload finished successfully!");
                // Now download it back
                downloadLargeDataExample();
            } else {
                System.err.println("💥 Large data upload failed!");
            }
        });
    }

    /**
     * Example: Download large data with progress tracking
     */
    public void downloadLargeDataExample() {
        CompletableFuture<String> downloadFuture = streamingCacheService.getLargeDataStream(
            "large-document-1",
            progress -> {
                System.out.println("📥 DOWNLOAD: " + progress);
                if (progress.hasError()) {
                    System.err.println("❌ Download error: " + progress.getMessage());
                } else if (progress.isCompleted()) {
                    System.out.println("✅ Download completed successfully!");
                }
            }
        );

        // Handle completion
        downloadFuture.thenAccept(data -> {
            if (data != null) {
                System.out.println("🎉 Downloaded data size: " + data.length() + " characters");
                System.out.println("First 100 chars: " + data.substring(0, Math.min(100, data.length())));
            } else {
                System.err.println("💥 Large data download failed!");
            }
        });
    }

    /**
     * Example: Clean up large data
     */
    public void cleanupLargeDataExample() {
        CompletableFuture<Boolean> cleanupFuture = streamingCacheService.removeLargeDataStream("large-document-1");

        cleanupFuture.thenAccept(success -> {
            if (success) {
                System.out.println("🧹 Successfully cleaned up large data and all chunks");
            } else {
                System.err.println("💥 Failed to cleanup large data");
            }
        });
    }

    /**
     * Integration example with SOAP service
     */
    public CompletableFuture<Boolean> cacheLargeSOAPResponse(String operationName, String userId, String largeXmlResponse) {
        String cacheKey = String.format("soap-response-%s-%s", operationName, userId);
        
        return streamingCacheService.putLargeDataStream(
            cacheKey,
            largeXmlResponse,
            progress -> {
                System.out.println("SOAP Response Caching: " + progress);
            }
        );
    }

    /**
     * Retrieve cached SOAP response
     */
    public CompletableFuture<String> getCachedSOAPResponse(String operationName, String userId) {
        String cacheKey = String.format("soap-response-%s-%s", operationName, userId);
        
        return streamingCacheService.getLargeDataStream(
            cacheKey,
            progress -> {
                System.out.println("SOAP Response Retrieval: " + progress);
            }
        );
    }
}