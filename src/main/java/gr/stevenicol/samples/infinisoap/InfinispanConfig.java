package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * Infinispan cache configuration for large string caching.
 * Uses Quarkus default RemoteCacheManager with custom cache setup.
 */
@ApplicationScoped
public class InfinispanConfig {

    @Inject
    RemoteCacheManager cacheManager;

    // Commented out to avoid conflicts with Camel Infinispan component
    // Cache will be created via Infinispan CLI or server configuration
    /*
    @PostConstruct
    void setupCache() {
        String cacheName = "large-string-cache";
        
        // Check if cache already exists
        if (cacheManager.getCacheNames().contains(cacheName)) {
            return;
        }
        
        // Cache configuration optimized for large strings
        String cacheConfig = """
            <distributed-cache name="%s">
                <encoding>
                    <key media-type="application/x-protostream"/>
                    <value media-type="application/x-protostream"/>
                </encoding>
                <memory max-size="2GB" when-full="REMOVE"/>
                <expiration lifespan="3600000" max-idle="1800000"/>
                <persistence>
                    <file-store path="large-string-cache-store" purge="false"/>
                </persistence>
            </distributed-cache>
            """.formatted(cacheName);

        // Create cache with configuration
        cacheManager.administration()
            .createCache(cacheName, new XMLStringConfiguration(cacheConfig));
    }
    */
}