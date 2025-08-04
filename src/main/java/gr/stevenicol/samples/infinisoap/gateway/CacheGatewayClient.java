package gr.stevenicol.samples.infinisoap.gateway;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import gr.stevenicol.samples.infinisoap.gateway.CacheGatewayService;
import gr.stevenicol.samples.infinisoap.gateway.CacheNotificationMetadata;
import gr.stevenicol.samples.infinisoap.gateway.GetCacheStatusRequest;
import gr.stevenicol.samples.infinisoap.gateway.GetCacheStatusResponse;
import gr.stevenicol.samples.infinisoap.gateway.NotifyCacheCompletionRequest;
import gr.stevenicol.samples.infinisoap.gateway.NotifyCacheCompletionResponse;
import gr.stevenicol.samples.infinisoap.gateway.GatewayProcessingFaultMessage;
import gr.stevenicol.samples.infinisoap.gateway.InvalidJobFaultMessage;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.concurrent.CompletableFuture;

/**
 * SOAP client for communicating with the Cache Gateway Service.
 * Used to notify external systems when cache operations complete.
 */
@ApplicationScoped
public class CacheGatewayClient {

    @ConfigProperty(name = "gateway.soap.endpoint", defaultValue = "http://localhost:8081/gateway/services/cache")
    String gatewayEndpoint;

    @ConfigProperty(name = "gateway.soap.timeout", defaultValue = "30000")
    int timeoutMs;

    private CacheGatewayService gatewayService;

    public void initializeClient() {
        if (gatewayService == null) {
            System.out.println("🔗 Initializing Gateway SOAP client for endpoint: " + gatewayEndpoint);
            
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(CacheGatewayService.class);
            factory.setAddress(gatewayEndpoint);
            
            gatewayService = (CacheGatewayService) factory.create();
            
            System.out.println("✅ Gateway SOAP client initialized successfully");
        }
    }

    /**
     * Notify the gateway service that cache operation has completed.
     * 
     * @param jobId Unique identifier for the cache job
     * @param cacheKey The key used to store data in cache
     * @param originalDataSize Size of the original data in bytes
     * @param totalChunks Number of chunks the data was split into
     * @param chunkSize Size of each chunk in bytes
     * @param dataType Type of data cached (e.g., "PERSONS_XML", "JSON", etc.)
     * @return CompletableFuture with notification result
     */
    public CompletableFuture<Boolean> notifyCacheCompletion(
            String jobId, 
            String cacheKey, 
            long originalDataSize, 
            int totalChunks, 
            int chunkSize,
            String dataType) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                initializeClient();
                
                System.out.println("📤 Notifying gateway: jobId=" + jobId + ", cacheKey=" + cacheKey);
                
                // Create request
                NotifyCacheCompletionRequest request = new NotifyCacheCompletionRequest();
                request.setJobId(jobId);
                
                // Create metadata
                CacheNotificationMetadata metadata = new CacheNotificationMetadata();
                metadata.setCacheKey(cacheKey);
                metadata.setOriginalDataSize(originalDataSize);
                metadata.setTotalChunks(totalChunks);
                metadata.setChunkSize(chunkSize);
                metadata.setDataType(dataType);
                metadata.setSourceSystem("INFINISPAN_STREAMING_CACHE");
                metadata.setRetentionPolicy("DEFAULT_TTL");
                metadata.setCompressionUsed(false);
                
                // Set timestamp
                try {
                    GregorianCalendar gcal = GregorianCalendar.from(LocalDateTime.now().atZone(ZoneId.systemDefault()));
                    XMLGregorianCalendar xmlDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
                    metadata.setStorageTimestamp(xmlDate);
                } catch (Exception e) {
                    System.err.println("⚠️ Warning: Could not set timestamp: " + e.getMessage());
                }
                
                request.setCacheMetadata(metadata);
                
                // Call gateway service
                NotifyCacheCompletionResponse response = gatewayService.notifyCacheCompletion(request);
                
                if (response.isSuccess()) {
                    System.out.println("✅ Gateway notification successful: " + response.getMessage());
                    if (response.getAcknowledgmentId() != null) {
                        System.out.println("📋 Acknowledgment ID: " + response.getAcknowledgmentId());
                    }
                    return true;
                } else {
                    System.err.println("❌ Gateway notification failed: " + response.getMessage());
                    return false;
                }
                
            } catch (InvalidJobFaultMessage e) {
                System.err.println("❌ Invalid job fault: " + e.getMessage());
                if (e.getFaultInfo() != null) {
                    System.err.println("   Job ID: " + e.getFaultInfo().getJobId());
                    System.err.println("   Reason: " + e.getFaultInfo().getReason());
                }
                return false;
                
            } catch (GatewayProcessingFaultMessage e) {
                System.err.println("❌ Gateway processing fault: " + e.getMessage());
                if (e.getFaultInfo() != null) {
                    System.err.println("   Error Code: " + e.getFaultInfo().getErrorCode());
                    System.err.println("   Error Message: " + e.getFaultInfo().getErrorMessage());
                    System.err.println("   Timestamp: " + e.getFaultInfo().getTimestamp());
                }
                return false;
                
            } catch (Exception e) {
                System.err.println("❌ Unexpected error notifying gateway: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Get the status of a cache job from the gateway service.
     * 
     * @param jobId Unique identifier for the cache job
     * @return CompletableFuture with job status response
     */
    public CompletableFuture<GetCacheStatusResponse> getCacheStatus(String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                initializeClient();
                
                System.out.println("📋 Getting cache status for jobId: " + jobId);
                
                GetCacheStatusRequest request = new GetCacheStatusRequest();
                request.setJobId(jobId);
                
                GetCacheStatusResponse response = gatewayService.getCacheStatus(request);
                
                System.out.println("📊 Cache status retrieved: " + response.getStatus() + " for job: " + response.getJobId());
                if (response.getLastUpdated() != null) {
                    System.out.println("🕒 Last updated: " + response.getLastUpdated());
                }
                
                return response;
                
            } catch (InvalidJobFaultMessage e) {
                System.err.println("❌ Invalid job fault for status request: " + e.getMessage());
                return null;
                
            } catch (GatewayProcessingFaultMessage e) {
                System.err.println("❌ Gateway processing fault for status request: " + e.getMessage());
                return null;
                
            } catch (Exception e) {
                System.err.println("❌ Unexpected error getting cache status: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Convenience method to create a unique job ID based on cache key and timestamp.
     * 
     * @param cacheKey The cache key
     * @return Unique job ID
     */
    public static String generateJobId(String cacheKey) {
        return "CACHE_JOB_" + cacheKey.replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis();
    }

    /**
     * Test connectivity to the gateway service.
     * 
     * @return true if gateway is reachable, false otherwise
     */
    public boolean testConnection() {
        try {
            initializeClient();
            
            // Try to get status for a test job - this will likely fail but confirms connectivity
            GetCacheStatusRequest testRequest = new GetCacheStatusRequest();
            testRequest.setJobId("CONNECTION_TEST_" + System.currentTimeMillis());
            
            gatewayService.getCacheStatus(testRequest);
            
            System.out.println("✅ Gateway connection test successful");
            return true;
            
        } catch (InvalidJobFaultMessage e) {
            // This is expected for a test job - means connection works
            System.out.println("✅ Gateway connection test successful (expected invalid job fault)");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Gateway connection test failed: " + e.getMessage());
            return false;
        }
    }
}