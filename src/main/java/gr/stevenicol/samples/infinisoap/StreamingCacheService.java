package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Streaming-enabled cache service for very large data (125-150MB).
 * Supports chunked storage, progress tracking, and memory-efficient operations.
 */
@ApplicationScoped
public class StreamingCacheService {

    private static final Logger log = LoggerFactory.getLogger(StreamingCacheService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Configurable chunk size (default 10MB)
    private static final int DEFAULT_CHUNK_SIZE = 10 * 1024 * 1024; // 10MB chunks
    private static final String CHUNK_KEY_PREFIX = "chunk:";
    private static final String METADATA_KEY_PREFIX = "meta:";

    @Inject
    ProducerTemplate producerTemplate;

    /**
     * Store large data using chunked streaming approach
     */
    public CompletableFuture<Boolean> putLargeDataStream(String key, String data, 
                                                        Consumer<StreamProgress> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
                byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
                int totalSize = dataBytes.length;
                int chunkSize = DEFAULT_CHUNK_SIZE;
                int totalChunks = (int) Math.ceil((double) totalSize / chunkSize);

                log.info("STREAM-PUT: Starting chunked upload for key={}, totalSize={} bytes, chunks={}", 
                         key, totalSize, totalChunks);

                // Store metadata first
                CacheMetadata metadata = new CacheMetadata(key, timestamp, totalSize, totalChunks, chunkSize);
                boolean metaSuccess = storeMetadata(key, metadata);
                if (!metaSuccess) {
                    progressCallback.accept(new StreamProgress(key, 0, totalChunks, "Failed to store metadata"));
                    return false;
                }

                // Stream chunks
                for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                    int startOffset = chunkIndex * chunkSize;
                    int endOffset = Math.min(startOffset + chunkSize, totalSize);
                    int currentChunkSize = endOffset - startOffset;

                    // Create chunk data
                    byte[] chunkData = new byte[currentChunkSize];
                    System.arraycopy(dataBytes, startOffset, chunkData, 0, currentChunkSize);
                    
                    // Store chunk
                    String chunkKey = CHUNK_KEY_PREFIX + key + ":" + chunkIndex;
                    CacheChunk chunk = new CacheChunk(chunkIndex, chunkData, timestamp);
                    
                    boolean chunkSuccess = storeChunk(chunkKey, chunk);
                    if (!chunkSuccess) {
                        progressCallback.accept(new StreamProgress(key, chunkIndex, totalChunks, 
                            "Failed to store chunk " + chunkIndex));
                        return false;
                    }

                    // Report progress
                    int progressPercent = (int) ((double) (chunkIndex + 1) / totalChunks * 100);
                    progressCallback.accept(new StreamProgress(key, chunkIndex + 1, totalChunks, 
                        "Uploaded chunk " + (chunkIndex + 1) + "/" + totalChunks + " (" + progressPercent + "%)"));
                }

                log.info("STREAM-PUT: ✅ Successfully stored all {} chunks for key={}", totalChunks, key);
                progressCallback.accept(new StreamProgress(key, totalChunks, totalChunks, "Upload completed successfully"));
                return true;

            } catch (Exception e) {
                log.error("STREAM-PUT: ❌ Failed to store data for key={}: {}", key, e.getMessage(), e);
                progressCallback.accept(new StreamProgress(key, 0, 0, "Upload failed: " + e.getMessage()));
                return false;
            }
        });
    }

    /**
     * Retrieve large data using chunked streaming approach
     */
    public CompletableFuture<String> getLargeDataStream(String key, Consumer<StreamProgress> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("STREAM-GET: Starting chunked download for key={}", key);

                // Get metadata first
                CacheMetadata metadata = getMetadata(key);
                if (metadata == null) {
                    progressCallback.accept(new StreamProgress(key, 0, 0, "Metadata not found"));
                    return null;
                }

                int totalChunks = metadata.getTotalChunks();
                byte[] reconstructedData = new byte[metadata.getTotalSize()];
                int currentOffset = 0;

                // Download chunks
                for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                    String chunkKey = CHUNK_KEY_PREFIX + key + ":" + chunkIndex;
                    CacheChunk chunk = getChunk(chunkKey);
                    
                    if (chunk == null) {
                        progressCallback.accept(new StreamProgress(key, chunkIndex, totalChunks, 
                            "Missing chunk " + chunkIndex));
                        return null;
                    }

                    // Copy chunk data to reconstructed array
                    byte[] chunkData = chunk.getData();
                    System.arraycopy(chunkData, 0, reconstructedData, currentOffset, chunkData.length);
                    currentOffset += chunkData.length;

                    // Report progress
                    int progressPercent = (int) ((double) (chunkIndex + 1) / totalChunks * 100);
                    progressCallback.accept(new StreamProgress(key, chunkIndex + 1, totalChunks, 
                        "Downloaded chunk " + (chunkIndex + 1) + "/" + totalChunks + " (" + progressPercent + "%)"));
                }

                String result = new String(reconstructedData, StandardCharsets.UTF_8);
                log.info("STREAM-GET: ✅ Successfully reconstructed data for key={}, size={} bytes", 
                         key, result.length());
                progressCallback.accept(new StreamProgress(key, totalChunks, totalChunks, "Download completed successfully"));
                
                return result;

            } catch (Exception e) {
                log.error("STREAM-GET: ❌ Failed to retrieve data for key={}: {}", key, e.getMessage(), e);
                progressCallback.accept(new StreamProgress(key, 0, 0, "Download failed: " + e.getMessage()));
                return null;
            }
        });
    }

    /**
     * Clean up chunked data (remove all chunks and metadata)
     */
    public CompletableFuture<Boolean> removeLargeDataStream(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get metadata to know how many chunks to remove
                CacheMetadata metadata = getMetadata(key);
                if (metadata == null) {
                    log.warn("STREAM-REMOVE: No metadata found for key={}", key);
                    return true; // Nothing to remove
                }

                int totalChunks = metadata.getTotalChunks();
                
                // Remove all chunks
                for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                    String chunkKey = CHUNK_KEY_PREFIX + key + ":" + chunkIndex;
                    removeChunk(chunkKey);
                }

                // Remove metadata
                removeMetadata(key);

                log.info("STREAM-REMOVE: ✅ Successfully removed {} chunks and metadata for key={}", 
                         totalChunks, key);
                return true;

            } catch (Exception e) {
                log.error("STREAM-REMOVE: ❌ Failed to remove data for key={}: {}", key, e.getMessage(), e);
                return false;
            }
        });
    }

    // Helper methods for cache operations
    private boolean storeMetadata(String key, CacheMetadata metadata) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("CamelInfinispanOperation", "PUT");
            headers.put("CamelInfinispanKey", METADATA_KEY_PREFIX + key);
            
            Object result = producerTemplate.requestBodyAndHeaders("direct:cache-put", metadata, headers);
            return "SUCCESS".equals(result);
        } catch (Exception e) {
            log.error("Failed to store metadata for key={}: {}", key, e.getMessage());
            return false;
        }
    }

    private CacheMetadata getMetadata(String key) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("CamelInfinispanOperation", "GET");
            headers.put("CamelInfinispanKey", METADATA_KEY_PREFIX + key);
            
            return producerTemplate.requestBodyAndHeaders("direct:cache-get", null, headers, CacheMetadata.class);
        } catch (Exception e) {
            log.error("Failed to get metadata for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    private boolean storeChunk(String chunkKey, CacheChunk chunk) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("CamelInfinispanOperation", "PUT");
            headers.put("CamelInfinispanKey", chunkKey);
            
            Object result = producerTemplate.requestBodyAndHeaders("direct:cache-put", chunk, headers);
            return "SUCCESS".equals(result);
        } catch (Exception e) {
            log.error("Failed to store chunk {}: {}", chunkKey, e.getMessage());
            return false;
        }
    }

    private CacheChunk getChunk(String chunkKey) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("CamelInfinispanOperation", "GET");
            headers.put("CamelInfinispanKey", chunkKey);
            
            return producerTemplate.requestBodyAndHeaders("direct:cache-get", null, headers, CacheChunk.class);
        } catch (Exception e) {
            log.error("Failed to get chunk {}: {}", chunkKey, e.getMessage());
            return null;
        }
    }

    private void removeMetadata(String key) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("CamelInfinispanOperation", "REMOVE");
            headers.put("CamelInfinispanKey", METADATA_KEY_PREFIX + key);
            
            producerTemplate.sendBodyAndHeaders("direct:cache-remove", null, headers);
        } catch (Exception e) {
            log.error("Failed to remove metadata for key={}: {}", key, e.getMessage());
        }
    }

    private void removeChunk(String chunkKey) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("CamelInfinispanOperation", "REMOVE");
            headers.put("CamelInfinispanKey", chunkKey);
            
            producerTemplate.sendBodyAndHeaders("direct:cache-remove", null, headers);
        } catch (Exception e) {
            log.error("Failed to remove chunk {}: {}", chunkKey, e.getMessage());
        }
    }
}