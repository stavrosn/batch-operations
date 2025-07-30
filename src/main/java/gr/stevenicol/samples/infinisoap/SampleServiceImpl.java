package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jws.WebService;
import samples.stevenicol.gr.soap.sampleservice.*;

@WebService(endpointInterface = "samples.stevenicol.gr.soap.sampleservice.SampleService",
           targetNamespace = "http://gr.stevenicol.samples/soap/SampleService",
           serviceName = "SampleService",
           portName = "SampleServicePort")
@ApplicationScoped
public class SampleServiceImpl extends AbstractInterceptedService implements SampleService {

    @Override
    public void addPerson(Person arg0) {
        System.out.println("addPerson called with: " + (arg0 != null ? arg0.getName() : "null"));
        
        // Enforced to use interceptor
        executeWithInterceptor("addPerson", arg0, "direct:addPerson");
    }

    @Override
    public Persons getPersons() {
        System.out.println("getPersons called");
        
        // Enforced to use interceptor
        Persons result = executeWithInterceptor("getPersons", null, "direct:getPersons", Persons.class);
        return result != null ? result : createDefaultPersons();
    }

    @Override
    public void updatePerson(String arg0, Person arg1) throws NoSuchPersonException {
        System.out.println("updatePerson called for: " + arg0 + " with data: " + (arg1 != null ? arg1.getName() : "null"));
        
        // Enforced to use interceptor
        UpdatePersonRequest request = new UpdatePersonRequest(arg0, arg1);
        executeWithInterceptor("updatePerson", request, "direct:updatePerson");
    }

    @Override
    public Person getPerson(String arg0) throws NoSuchPersonException {
        System.out.println("getPerson called for: " + arg0);
        
        // Enforced to use interceptor
        Person result = executeWithInterceptor("getPerson", arg0, "direct:getPerson", Person.class);
        return result != null ? result : createDefaultPerson(arg0);
    }

    @Override
    public void deletePerson(String arg0) throws NoSuchPersonException {
        System.out.println("deletePerson called for: " + arg0);
        
        // Enforced to use interceptor
        executeWithInterceptor("deletePerson", arg0, "direct:deletePerson");
    }

    @Override
    public boolean writePersonsToFile() {
        System.out.println("🚀 writePersonsToFile called - streaming persons from MS SQL to file");
        
        // Enforced to use interceptor - returns boolean result
        Boolean result = executeWithInterceptor("writePersonsToFile", null, "direct:writePersonsToFile", Boolean.class);
        return result != null ? result : false;
    }

    @Override
    public boolean writePersonsToCache() {
        System.out.println("🚀 writePersonsToCache called - streaming persons from MS SQL to Infinispan");
        
        // Enforced to use interceptor - returns boolean result
        Boolean result = executeWithInterceptor("writePersonsToCache", null, "direct:writePersonsToCache", Boolean.class);
        return result != null ? result : false;
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

    // Helper class for updatePerson parameters
    public static class UpdatePersonRequest {
        private String personName;
        private Person person;

        public UpdatePersonRequest(String personName, Person person) {
            this.personName = personName;
            this.person = person;
        }

        public String getPersonName() { return personName; }
        public Person getPerson() { return person; }
    }
}