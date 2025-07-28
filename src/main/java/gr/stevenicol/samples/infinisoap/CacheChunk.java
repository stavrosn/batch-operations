package gr.stevenicol.samples.infinisoap;

import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * Represents a chunk of data in the chunked storage approach.
 * Each chunk contains a portion of the original large data.
 */
@Proto
public class CacheChunk {

    @ProtoField(number = 1, defaultValue = "0")
    public int chunkIndex;

    @ProtoField(number = 2)
    public byte[] data;

    @ProtoField(number = 3)
    public String timestamp;

    @ProtoFactory
    public CacheChunk(int chunkIndex, byte[] data, String timestamp) {
        this.chunkIndex = chunkIndex;
        this.data = data;
        this.timestamp = timestamp;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public byte[] getData() {
        return data;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "CacheChunk{" +
                "chunkIndex=" + chunkIndex +
                ", dataSize=" + (data != null ? data.length : 0) + " bytes" +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}