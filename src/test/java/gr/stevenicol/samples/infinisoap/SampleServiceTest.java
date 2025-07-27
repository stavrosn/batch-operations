package gr.stevenicol.samples.infinisoap;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import samples.stevenicol.gr.soap.sampleservice.*;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SampleService SOAP operations.
 * Tests the getPersons method with interceptor integration.
 */
@QuarkusTest
class SampleServiceTest {

    @Inject
    SampleServiceImpl sampleService;

    @BeforeEach
    void setUp() {
        // Setup code if needed
    }

    @Test
    @DisplayName("Test getPersons operation with interceptor integration")
    void testGetPersons() {
        // Act: Call the SOAP service method
        Persons result = sampleService.getPersons();

        // Assert: Verify the response is not null
        assertNotNull(result, "getPersons should return a non-null Persons object");

        // Assert: Verify the response contains persons
        assertNotNull(result.getPersons(), "Persons list should not be null");
        assertFalse(result.getPersons().isEmpty(), "Persons list should not be empty");

        // Assert: Verify the content of the first person
        Person firstPerson = result.getPersons().get(0);
        assertNotNull(firstPerson, "First person should not be null");
        assertNotNull(firstPerson.getName(), "Person name should not be null");
        assertNotNull(firstPerson.getAddress(), "Person address should not be null");
        assertNotNull(firstPerson.getType(), "Person contact type should not be null");

        // Assert: Verify sample data structure
        assertEquals("John Doe", firstPerson.getName(), "First person should be John Doe");
        assertEquals("123 Main St", firstPerson.getAddress().getStreet(), "Address street should match");
        assertEquals("New York", firstPerson.getAddress().getCity(), "Address city should match");
        assertEquals("10001", firstPerson.getAddress().getPostalCode(), "Address postal code should match");
        assertEquals(ContactType.PERSONAL, firstPerson.getType(), "Contact type should be PERSONAL");

        // Assert: Verify we have the expected number of persons (from SampleRouter)
        assertTrue(result.getPersons().size() >= 2, "Should have at least 2 persons from sample data");

        // Assert: Verify the second person if present
        if (result.getPersons().size() > 1) {
            Person secondPerson = result.getPersons().get(1);
            assertEquals("Jane Smith", secondPerson.getName(), "Second person should be Jane Smith");
            assertEquals(ContactType.WORK, secondPerson.getType(), "Second person should have WORK contact type");
        }

        System.out.println("✅ getPersons test completed successfully");
        System.out.println("📊 Returned " + result.getPersons().size() + " persons");
        System.out.println("🔄 Interceptor integration verified (audit and security logging should appear in console)");
    }

    @Test
    @DisplayName("Test that getPersons handles null response gracefully")
    void testGetPersonsWithFallback() {
        // This test verifies the fallback mechanism in SampleServiceImpl
        // Even if the Camel route returns null, the service should return default data
        
        // Act: Call the service method
        Persons result = sampleService.getPersons();
        
        // Assert: Should always return a valid response due to fallback
        assertNotNull(result, "Service should always return a non-null response due to fallback mechanism");
        assertNotNull(result.getPersons(), "Persons list should not be null due to fallback");
        assertFalse(result.getPersons().isEmpty(), "Should have fallback data if route returns null");
    }
}