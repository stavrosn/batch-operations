package gr.stevenicol.samples.infinisoap;

import samples.stevenicol.gr.soap.sampleservice.*;
import jakarta.jws.WebService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Alternative implementation using @Intercepted annotation approach
 * This shows how the annotation-based enforcement would work
 */
@WebService(endpointInterface = "samples.stevenicol.gr.soap.sampleservice.SampleService",
           targetNamespace = "http://gr.stevenicol.samples/soap/SampleService",
           serviceName = "SampleServiceAnnotated",
           portName = "SampleServiceAnnotatedPort")
@ApplicationScoped
public class SampleServiceAnnotatedImpl implements SampleService {

    @Override
    @Intercepted(targetRoute = "direct:addPerson", operation = "addPerson")
    public void addPerson(Person arg0) {
        System.out.println("addPerson called with: " + (arg0 != null ? arg0.getName() : "null"));
        // No manual interceptor call needed - annotation handles it automatically
    }

    @Override
    @Intercepted(targetRoute = "direct:getPersons", operation = "getPersons")
    public Persons getPersons() {
        System.out.println("getPersons called");
        // The interceptor automatically routes through direct:soap-interceptor
        // and then to direct:getPersons, returning the result
        return createDefaultPersons(); // This becomes unreachable due to interceptor
    }

    @Override
    @Intercepted(targetRoute = "direct:updatePerson", operation = "updatePerson")
    public void updatePerson(String arg0, Person arg1) throws NoSuchPersonException {
        System.out.println("updatePerson called for: " + arg0);
        // Interceptor handles the routing automatically
    }

    @Override
    @Intercepted(targetRoute = "direct:getPerson", operation = "getPerson")
    public Person getPerson(String arg0) throws NoSuchPersonException {
        System.out.println("getPerson called for: " + arg0);
        // Interceptor handles routing and returns the result from Camel route
        return createDefaultPerson(arg0); // This becomes unreachable due to interceptor
    }

    @Override
    @Intercepted(targetRoute = "direct:deletePerson", operation = "deletePerson")
    public void deletePerson(String arg0) throws NoSuchPersonException {
        System.out.println("deletePerson called for: " + arg0);
        // Interceptor handles the routing automatically
    }

    @Override
    public boolean writePersonsToCache() {
        return false;
    }

    @Override
    public boolean writePersonsToFile() {
        return false;
    }

    // Helper methods remain the same
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