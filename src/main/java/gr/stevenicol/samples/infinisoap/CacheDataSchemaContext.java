package gr.stevenicol.samples.infinisoap;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;

/**
 * Protobuf schema context for CacheData following Quarkus best practices.
 * This generates the .proto schema file and registers serializers.
 */
@ProtoSchema(
    includeClasses = { CacheData.class, CacheMetadata.class, CacheChunk.class },
    schemaFileName = "cache-data.proto",
    schemaFilePath = "proto/",
    schemaPackageName = "gr.stevenicol.samples.infinisoap"
)
public interface CacheDataSchemaContext extends GeneratedSchema {
}