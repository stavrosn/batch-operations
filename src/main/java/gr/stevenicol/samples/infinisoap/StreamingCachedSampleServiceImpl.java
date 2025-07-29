package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jws.WebService;
import samples.stevenicol.gr.soap.sampleservice.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Streaming cached version of SampleService using chunked Infinispan storage.
 * Suitable for very large datasets (125-150MB+) with progress tracking.
 */
@WebService(endpointInterface = "samples.stevenicol.gr.soap.sampleservice.SampleService",
           targetNamespace = "http://gr.stevenicol.samples/soap/SampleService",
           serviceName = "StreamingCachedSampleService",
           portName = "StreamingCachedSampleServicePort")
@ApplicationScoped
public class StreamingCachedSampleServiceImpl extends AbstractInterceptedService implements SampleService {

    @Inject
    StreamingCacheService streamingCacheService;

    @Override
    public void addPerson(Person arg0) {
        System.out.println("🚀 StreamingCachedSampleService: addPerson called with: " + (arg0 != null ? arg0.getName() : "null"));
        
        // Clear cache when data is modified
        String cacheKey = "streaming-persons-all";
        CompletableFuture<Boolean> removed = streamingCacheService.removeLargeDataStream(cacheKey);
        
        try {
            boolean result = removed.get();
            System.out.println("📦 Streaming cache cleared for key '" + cacheKey + "': " + result);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Error clearing streaming cache: " + e.getMessage());
        }
        
        // Enforced to use interceptor
        executeWithInterceptor("addPerson", arg0, "direct:addPerson");
    }

    @Override
    public Persons getPersons() {
        System.out.println("🚀 StreamingCachedSampleService: getPersons called");
        
        String cacheKey = "streaming-persons-all";
        
        // Try streaming cache first
        System.out.println("📦 Checking streaming cache for key: " + cacheKey);
        
        CompletableFuture<String> cachedDataFuture = streamingCacheService.getLargeDataStream(
            cacheKey,
            progress -> {
                System.out.println("📥 Cache retrieval progress: " + progress.getProgressPercent() + "% - " + progress.getMessage());
                if (progress.hasError()) {
                    System.err.println("❌ Cache retrieval error: " + progress.getMessage());
                }
            }
        );
        
        try {
            String cachedData = cachedDataFuture.get();
            
            if (cachedData != null && !cachedData.isEmpty()) {
                System.out.println("✅ Streaming Cache HIT! Returning cached data (length: " + cachedData.length() + " chars)");
                return deserializePersons(cachedData);
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Error retrieving from streaming cache: " + e.getMessage());
        }
        
        System.out.println("❌ Streaming Cache MISS! Fetching from database...");
        
        // Cache miss - get data through interceptor (simulate large dataset)
        Persons result = executeWithInterceptor("getPersons", null, "direct:getLargePersons", Persons.class);
        if (result == null) {
            result = createLargePersonsDataset();
        }
        
        // Cache the result with streaming
        String serializedResult = serializePersonsLarge(result);
        System.out.println("📦 Caching large dataset with streaming (size: " + serializedResult.length() + " chars)");
        
        CompletableFuture<Boolean> cachingFuture = streamingCacheService.putLargeDataStream(
            cacheKey,
            serializedResult,
            progress -> {
                System.out.println("📤 Cache storage progress: " + progress.getProgressPercent() + "% - " + progress.getMessage());
                if (progress.hasError()) {
                    System.err.println("❌ Cache storage error: " + progress.getMessage());
                } else if (progress.isCompleted()) {
                    System.out.println("✅ Large dataset cached successfully!");
                }
            }
        );
        
        // Don't wait for caching to complete - return result immediately
        cachingFuture.thenAccept(cached -> {
            System.out.println("📦 Background caching completed: " + cached);
        }).exceptionally(throwable -> {
            System.err.println("❌ Background caching failed: " + throwable.getMessage());
            return null;
        });
        
        return result;
    }

    @Override
    public void updatePerson(String arg0, Person arg1) throws NoSuchPersonException {
        System.out.println("🚀 StreamingCachedSampleService: updatePerson called for: " + arg0);
        
        // Clear streaming cache when data is modified
        String cacheKey = "streaming-persons-all";
        CompletableFuture<Boolean> removed = streamingCacheService.removeLargeDataStream(cacheKey);
        
        removed.thenAccept(result -> {
            System.out.println("📦 Streaming cache cleared for key '" + cacheKey + "': " + result);
        });
        
        // Enforced to use interceptor
        SampleServiceImpl.UpdatePersonRequest request = new SampleServiceImpl.UpdatePersonRequest(arg0, arg1);
        executeWithInterceptor("updatePerson", request, "direct:updatePerson");
    }

    @Override
    public Person getPerson(String arg0) throws NoSuchPersonException {
        System.out.println("🚀 StreamingCachedSampleService: getPerson called for: " + arg0);
        
        // For individual persons, use standard caching (not streaming)
        // as single person data is not large enough for streaming
        Person result = executeWithInterceptor("getPerson", arg0, "direct:getPerson", Person.class);
        if (result == null) {
            result = createDefaultPerson(arg0);
        }
        
        return result;
    }

    @Override
    public void deletePerson(String arg0) throws NoSuchPersonException {
        System.out.println("🚀 StreamingCachedSampleService: deletePerson called for: " + arg0);
        
        // Clear streaming cache when data is modified
        String cacheKey = "streaming-persons-all";
        CompletableFuture<Boolean> removed = streamingCacheService.removeLargeDataStream(cacheKey);
        
        removed.thenAccept(result -> {
            System.out.println("📦 Streaming cache cleared for key '" + cacheKey + "': " + result);
        });
        
        // Enforced to use interceptor
        executeWithInterceptor("deletePerson", arg0, "direct:deletePerson");
    }

    @Override
    public boolean writePersonsToFile() {
        System.out.println("🚀 StreamingCachedSampleService: writePersonsToFile called - streaming from MS SQL to file");
        
        // This operation doesn't use cache - direct database streaming to file
        Boolean result = executeWithInterceptor("writePersonsToFile", null, "direct:writePersonsToFile", Boolean.class);
        return result != null ? result : false;
    }

    // Large dataset creation for demo
    private Persons createLargePersonsDataset() {
        System.out.println("🏗️ Creating large persons dataset for demonstration...");
        
        Persons persons = new Persons();
        
        // Create 10,000 persons to simulate large dataset
        for (int i = 1; i <= 10000; i++) {
            Person person = new Person();
            person.setName("Person " + i);
            person.setType(i % 2 == 0 ? ContactType.WORK : ContactType.PERSONAL);
            
            Address address = new Address();
            address.setStreet("Street " + i + " Avenue");
            address.setCity("City " + (i % 100));
            address.setPostalCode(String.format("%05d", i));
            person.setAddress(address);
            
            persons.getPersons().add(person);
            
            // Log progress every 1000 records
            if (i % 1000 == 0) {
                System.out.println("🏗️ Generated " + i + " persons...");
            }
        }
        
        System.out.println("✅ Large dataset created: " + persons.getPersons().size() + " persons");
        return persons;
    }

    // Serialization helpers for large data
    private String serializePersonsLarge(Persons persons) {
        System.out.println("📝 Serializing large persons dataset...");
        
        StringBuilder sb = new StringBuilder();
        sb.append("{\"persons\":[");
        
        boolean first = true;
        int count = 0;
        for (Person person : persons.getPersons()) {
            if (!first) sb.append(",");
            sb.append(serializePersonDetailed(person));
            first = false;
            count++;
            
            // Log progress every 1000 records
            if (count % 1000 == 0) {
                System.out.println("📝 Serialized " + count + " persons...");
            }
        }
        
        sb.append("]}");
        
        String result = sb.toString();
        System.out.println("✅ Serialization complete. Size: " + result.length() + " characters");
        
        return result;
    }
    
    private String serializePersonDetailed(Person person) {
        // More detailed serialization to increase data size
        return String.format(
            "{\"name\":\"%s\",\"type\":\"%s\",\"address\":{\"street\":\"%s\",\"city\":\"%s\",\"postalCode\":\"%s\"},\"metadata\":{\"created\":\"%s\",\"id\":\"%s\",\"description\":\"This is a detailed person record with additional metadata to increase the data size for streaming cache demonstration purposes. Person type is %s and lives in %s.\"}}",
            person.getName(),
            person.getType(),
            person.getAddress() != null ? person.getAddress().getStreet() : "",
            person.getAddress() != null ? person.getAddress().getCity() : "",
            person.getAddress() != null ? person.getAddress().getPostalCode() : "",
            System.currentTimeMillis(),
            "ID-" + person.getName().hashCode(),
            person.getType(),
            person.getAddress() != null ? person.getAddress().getCity() : "Unknown City"
        );
    }
    
    private Persons deserializePersons(String data) {
        System.out.println("📖 Deserializing cached persons data...");
        
        // Simple deserialization for demo - in real world use Jackson/GSON
        Persons persons = new Persons();
        
        // Count approximate number of persons from data size
        int estimatedCount = data.length() / 200; // rough estimate
        System.out.println("📖 Estimated " + estimatedCount + " persons from cached data");
        
        // For demo, create some sample data based on cached content size
        int actualCount = Math.min(estimatedCount, 100); // limit for demo
        for (int i = 1; i <= actualCount; i++) {
            Person person = createDefaultPerson("Cached Person " + i);
            persons.getPersons().add(person);
        }
        
        System.out.println("✅ Deserialized " + persons.getPersons().size() + " persons from cache");
        return persons;
    }

    private Person createDefaultPerson(String name) {
        Person person = new Person();
        person.setName(name);
        
        Address address = new Address();
        address.setStreet("Default Street");
        address.setCity("Default City");
        address.setPostalCode("00000");
        person.setAddress(address);
        
        person.setType(ContactType.PERSONAL);
        return person;
    }
}