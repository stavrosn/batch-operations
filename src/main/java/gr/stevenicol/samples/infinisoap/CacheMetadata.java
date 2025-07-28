package gr.stevenicol.samples.infinisoap;

import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * Metadata for chunked cache entries.
 * Stores information about the original data and how it was chunked.
 */
@Proto
public class CacheMetadata {

    @ProtoField(number = 1)
    public String originalKey;

    @ProtoField(number = 2)
    public String timestamp;

    @ProtoField(number = 3, defaultValue = "0")
    public int totalSize;

    @ProtoField(number = 4, defaultValue = "0")
    public int totalChunks;

    @ProtoField(number = 5, defaultValue = "0")
    public int chunkSize;

    @ProtoFactory
    public CacheMetadata(String originalKey, String timestamp, int totalSize, int totalChunks, int chunkSize) {
        this.originalKey = originalKey;
        this.timestamp = timestamp;
        this.totalSize = totalSize;
        this.totalChunks = totalChunks;
        this.chunkSize = chunkSize;
    }

    public String getOriginalKey() {
        return originalKey;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public String toString() {
        return "CacheMetadata{" +
                "originalKey='" + originalKey + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", totalSize=" + totalSize +
                ", totalChunks=" + totalChunks +
                ", chunkSize=" + chunkSize +
                '}';
    }
}