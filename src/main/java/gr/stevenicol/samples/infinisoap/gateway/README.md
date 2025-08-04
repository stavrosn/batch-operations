# Gateway SOAP Client

This directory contains the SOAP client code for communicating with the Cache Gateway Service.

## Generated Classes

The following classes will be generated from `gateway.wsdl` using CXF wsdl2java:

**Target Package:** `gr.stevenicol.samples.gateway`

- `CacheGatewayService.java` - Service interface
- `CacheNotificationMetadata.java` - Cache metadata complex type
- `NotifyCacheCompletionRequest.java` - Notification request
- `NotifyCacheCompletionResponse.java` - Notification response
- `GetCacheStatusRequest.java` - Status request
- `GetCacheStatusResponse.java` - Status response
- `CacheJobStatus.java` - Status enumeration
- `InvalidJobFault_Exception.java` - Invalid job fault
- `GatewayProcessingFault_Exception.java` - Processing fault

## Quarkus CXF Configuration

The gateway client classes are generated automatically by **Quarkus CXF extension** during compilation.

Configuration in `application.properties`:

```properties
# Include both WSDLs for code generation
quarkus.cxf.codegen.wsdl2java.includes=wsdl/SampleService.wsdl,wsdl/gateway.wsdl

# Map gateway namespace to specific package
quarkus.cxf.codegen.wsdl2java.additional-params.gateway.wsdl=-p,http://gateway.infinisoap.samples.stevenicol.gr/=gr.stevenicol.samples.gateway
```

**Benefits of Quarkus CXF approach:**
- ✅ Integrated with Quarkus build process
- ✅ Automatic dependency injection for SOAP clients
- ✅ Better integration with Quarkus dev mode
- ✅ Native compilation support
- ✅ No additional Maven plugin configuration needed

## Usage

1. **Configure Gateway Endpoint** in `application.properties`:
   ```properties
   gateway.soap.endpoint=http://localhost:8081/gateway/services/cache
   gateway.notifications.enabled=true
   ```

2. **Use CacheNotificationService** (High-level API):
   ```java
   @Inject
   CacheNotificationService notificationService;
   
   // Notify gateway when caching completes
   notificationService.notifyPersonsDataCached(cacheMetadata)
       .thenAccept(success -> {
           if (success) {
               System.out.println("Gateway notified successfully");
           }
       });
   ```

3. **Use CacheGatewayClient** (Low-level API):
   ```java
   @Inject
   CacheGatewayClient gatewayClient;
   
   // Generate job ID and notify
   String jobId = CacheGatewayClient.generateJobId("my-cache-key");
   gatewayClient.notifyCacheCompletion(jobId, "cache-key", 1024000, 10, 102400, "PERSONS_XML");
   ```

## Integration Flow

1. **SOAP Service** → Cache large data using `StreamingCacheService`
2. **Background Caching** → Data is chunked and stored in Infinispan
3. **Cache Completion** → `StreamingCachedSampleServiceImpl` detects completion
4. **Gateway Notification** → `CacheNotificationService` calls gateway SOAP service
5. **External Processing** → Gateway service can trigger downstream processing

## Error Handling

The client handles the following fault types:
- `InvalidJobFault` - Invalid job ID or parameters
- `GatewayProcessingFault` - Gateway service errors
- Connection timeouts and network errors

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `gateway.soap.endpoint` | `http://localhost:8081/gateway/services/cache` | Gateway service URL |
| `gateway.soap.timeout` | `30000` | Request timeout in milliseconds |
| `gateway.notifications.enabled` | `true` | Enable/disable notifications |
| `gateway.notifications.async` | `true` | Use async notifications |
| `gateway.soap.retry.attempts` | `3` | Number of retry attempts |
| `gateway.soap.retry.delay` | `1000` | Retry delay in milliseconds |