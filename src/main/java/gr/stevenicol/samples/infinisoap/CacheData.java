package gr.stevenicol.samples.infinisoap;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.Proto;

/**
 * Protobuf class for caching large string data with timestamp.
 * This class is optimized for storing large strings (125-150MB) efficiently.
 */
@Proto
public class CacheData {

    @ProtoField(number = 1)
    public String dateString;

    @ProtoField(number = 2)
    public byte[] data;

    @ProtoFactory
    public CacheData(String dateString, byte[] data) {
        this.dateString = dateString;
        this.data = data;
    }

    // Keep getters for compatibility
    public String getDateString() {
        return dateString;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "CacheData{" +
                "dateString='" + dateString + '\'' +
                ", dataSize=" + (data != null ? data.length : 0) + " characters" +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheData cacheData = (CacheData) o;
        return java.util.Objects.equals(dateString, cacheData.dateString) &&
               java.util.Objects.equals(data.length, cacheData.data.length);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(dateString, data.length);
    }
}