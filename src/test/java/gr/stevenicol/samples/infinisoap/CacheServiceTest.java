package gr.stevenicol.samples.infinisoap;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import samples.stevenicol.gr.soap.sampleservice.Persons;

import java.util.concurrent.ExecutionException;

/**
 * Test class to demonstrate both caching implementations
 */
@QuarkusTest
public class CacheServiceTest {

    @Inject
    CachedSampleServiceImpl cachedService;

    @Inject
    StreamingCachedSampleServiceImpl streamingCachedService;

    @Test
    public void testStandardCaching() {
        System.out.println("\n🧪 ===== TESTING STANDARD CACHING =====");
        
        // First call - should be cache miss
        System.out.println("\n📋 First call (should be cache miss):");
        Persons persons1 = cachedService.getPersons();
        System.out.println("✅ Returned " + persons1.getPersons().size() + " persons");
        
        // Second call - should be cache hit
        System.out.println("\n📋 Second call (should be cache hit):");
        Persons persons2 = cachedService.getPersons();
        System.out.println("✅ Returned " + persons2.getPersons().size() + " persons");
        
        System.out.println("\n✅ Standard caching test completed!");
    }

    @Test
    public void testStreamingCaching() throws ExecutionException, InterruptedException {
        System.out.println("\n🧪 ===== TESTING STREAMING CACHING =====");
        
        // First call - should be cache miss and generate large dataset
        System.out.println("\n📋 First call (should be cache miss and generate large dataset):");
        Persons persons1 = streamingCachedService.getPersons();
        System.out.println("✅ Returned " + persons1.getPersons().size() + " persons");
        
        // Wait a moment for background caching to complete
        System.out.println("\n⏳ Waiting for background caching to complete...");
        Thread.sleep(2000);
        
        // Second call - should be cache hit with streaming retrieval
        System.out.println("\n📋 Second call (should be streaming cache hit):");
        Persons persons2 = streamingCachedService.getPersons();
        System.out.println("✅ Returned " + persons2.getPersons().size() + " persons");
        
        System.out.println("\n✅ Streaming caching test completed!");
    }
}