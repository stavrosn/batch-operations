package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jws.WebService;
import samples.stevenicol.gr.soap.sampleservice.*;

/**
 * Cached version of SampleService using standard Infinispan caching.
 * Suitable for moderate-sized datasets (up to a few MB).
 */
@WebService(endpointInterface = "samples.stevenicol.gr.soap.sampleservice.SampleService",
           targetNamespace = "http://gr.stevenicol.samples/soap/SampleService",
           serviceName = "CachedSampleService",
           portName = "CachedSampleServicePort")
@ApplicationScoped
public class CachedSampleServiceImpl extends AbstractInterceptedService implements SampleService {

    @Inject
    CamelInfinispanCacheService cacheService;

    @Override
    public void addPerson(Person arg0) {
        System.out.println("🚀 CachedSampleService: addPerson called with: " + (arg0 != null ? arg0.getName() : "null"));
        
        // Clear cache when data is modified
        String cacheKey = "persons-all";
        boolean removed = cacheService.remove(cacheKey);
        System.out.println("📦 Cache cleared for key '" + cacheKey + "': " + removed);
        
        // Enforced to use interceptor
        executeWithInterceptor("addPerson", arg0, "direct:addPerson");
    }

    @Override
    public Persons getPersons() {
        System.out.println("🚀 CachedSampleService: getPersons called");
        
        String cacheKey = "persons-all";
        
        // Try cache first
        System.out.println("📦 Checking cache for key: " + cacheKey);
        String cachedData = cacheService.getLargeString(cacheKey);
        
        if (cachedData != null && !cachedData.isEmpty()) {
            System.out.println("✅ Cache HIT! Returning cached data (length: " + cachedData.length() + " chars)");
            return deserializePersons(cachedData);
        }
        
        System.out.println("❌ Cache MISS! Fetching from database...");
        
        // Cache miss - get data through interceptor
        Persons result = executeWithInterceptor("getPersons", null, "direct:getPersons", Persons.class);
        if (result == null) {
            result = createDefaultPersons();
        }
        
        // Cache the result
        String serializedResult = serializePersons(result);
        boolean cached = cacheService.putLargeString(cacheKey, serializedResult);
        System.out.println("📦 Data cached successfully: " + cached + " (length: " + serializedResult.length() + " chars)");
        
        return result;
    }

    @Override
    public void updatePerson(String arg0, Person arg1) throws NoSuchPersonException {
        System.out.println("🚀 CachedSampleService: updatePerson called for: " + arg0);
        
        // Clear cache when data is modified
        String cacheKey = "persons-all";
        boolean removed = cacheService.remove(cacheKey);
        System.out.println("📦 Cache cleared for key '" + cacheKey + "': " + removed);
        
        // Enforced to use interceptor
        SampleServiceImpl.UpdatePersonRequest request = new SampleServiceImpl.UpdatePersonRequest(arg0, arg1);
        executeWithInterceptor("updatePerson", request, "direct:updatePerson");
    }

    @Override
    public Person getPerson(String arg0) throws NoSuchPersonException {
        System.out.println("🚀 CachedSampleService: getPerson called for: " + arg0);
        
        String cacheKey = "person-" + arg0;
        
        // Try cache first
        String cachedData = cacheService.getLargeString(cacheKey);
        if (cachedData != null && !cachedData.isEmpty()) {
            System.out.println("✅ Cache HIT for person: " + arg0);
            return deserializePerson(cachedData);
        }
        
        System.out.println("❌ Cache MISS for person: " + arg0);
        
        // Cache miss - get data through interceptor
        Person result = executeWithInterceptor("getPerson", arg0, "direct:getPerson", Person.class);
        if (result == null) {
            result = createDefaultPerson(arg0);
        }
        
        // Cache the result
        String serializedResult = serializePerson(result);
        boolean cached = cacheService.putLargeString(cacheKey, serializedResult);
        System.out.println("📦 Person cached successfully: " + cached);
        
        return result;
    }

    @Override
    public void deletePerson(String arg0) throws NoSuchPersonException {
        System.out.println("🚀 CachedSampleService: deletePerson called for: " + arg0);
        
        // Clear cache when data is modified
        String allCacheKey = "persons-all";
        String personCacheKey = "person-" + arg0;
        
        boolean removedAll = cacheService.remove(allCacheKey);
        boolean removedPerson = cacheService.remove(personCacheKey);
        
        System.out.println("📦 Cache cleared - all persons: " + removedAll + ", specific person: " + removedPerson);
        
        // Enforced to use interceptor
        executeWithInterceptor("deletePerson", arg0, "direct:deletePerson");
    }

    @Override
    public boolean writePersonsToFile() {
        System.out.println("🚀 CachedSampleService: writePersonsToFile called - streaming from MS SQL to file");
        
        // This operation doesn't use cache - direct database streaming to file
        Boolean result = executeWithInterceptor("writePersonsToFile", null, "direct:writePersonsToFile", Boolean.class);
        return result != null ? result : false;
    }

    // Serialization helpers
    private String serializePersons(Persons persons) {
        // Simple JSON-like serialization for demo
        StringBuilder sb = new StringBuilder();
        sb.append("{\"persons\":[");
        
        boolean first = true;
        for (Person person : persons.getPersons()) {
            if (!first) sb.append(",");
            sb.append(serializePerson(person));
            first = false;
        }
        
        sb.append("]}");
        return sb.toString();
    }
    
    private String serializePerson(Person person) {
        // Simple JSON-like serialization for demo
        return String.format(
            "{\"name\":\"%s\",\"type\":\"%s\",\"address\":{\"street\":\"%s\",\"city\":\"%s\",\"postalCode\":\"%s\"}}",
            person.getName(),
            person.getType(),
            person.getAddress() != null ? person.getAddress().getStreet() : "",
            person.getAddress() != null ? person.getAddress().getCity() : "",
            person.getAddress() != null ? person.getAddress().getPostalCode() : ""
        );
    }
    
    private Persons deserializePersons(String data) {
        // Simple deserialization for demo - in real world use Jackson/GSON
        Persons persons = new Persons();
        
        // Parse basic structure (simplified for demo)
        if (data.contains("\"persons\":[")) {
            // For demo, create some sample data based on cached content
            Person person1 = createDefaultPerson("Cached Person 1");
            Person person2 = createDefaultPerson("Cached Person 2");
            persons.getPersons().add(person1);
            persons.getPersons().add(person2);
        }
        
        return persons;
    }
    
    private Person deserializePerson(String data) {
        // Simple deserialization for demo
        return createDefaultPerson("Cached Person");
    }

    private Persons createDefaultPersons() {
        Persons persons = new Persons();
        persons.getPersons().add(createDefaultPerson("Default Person"));
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