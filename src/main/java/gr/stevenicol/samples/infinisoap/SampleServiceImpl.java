package gr.stevenicol.samples.infinisoap;

import samples.stevenicol.gr.soap.sampleservice.*;
import jakarta.jws.WebService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;

@WebService(endpointInterface = "samples.stevenicol.gr.soap.sampleservice.SampleService",
           targetNamespace = "http://gr.stevenicol.samples/soap/SampleService",
           serviceName = "SampleService",
           portName = "SampleServicePort")
@ApplicationScoped
public class SampleServiceImpl implements SampleService {

    @Inject
    ProducerTemplate producerTemplate;

    @Override
    public void addPerson(Person arg0) {
        System.out.println("addPerson called with: " + (arg0 != null ? arg0.getName() : "null"));
        
        // Send to Camel route for processing
        producerTemplate.sendBodyAndHeader("direct:addPerson", arg0, "operation", "addPerson");
    }

    @Override
    public Persons getPersons() {
        System.out.println("getPersons called");
        
        // Create ServiceRequest for interceptor
        ServiceRequest serviceRequest = new ServiceRequest("getPersons", null, "direct:getPersons");
        serviceRequest.setUserId("demo-user"); // In real scenario, get from security context
        serviceRequest.setSessionId("session-" + System.currentTimeMillis());
        serviceRequest.addHeader("operation", "getPersons");
        
        // Send to interceptor route first
        Persons result = producerTemplate.requestBodyAndHeaders("direct:soap-interceptor", 
            serviceRequest, 
            serviceRequest.getHeaders(), 
            Persons.class);
        return result != null ? result : createDefaultPersons();
    }

    @Override
    public void updatePerson(String arg0, Person arg1) throws NoSuchPersonException {
        System.out.println("updatePerson called for: " + arg0 + " with data: " + (arg1 != null ? arg1.getName() : "null"));
        
        // Send to Camel route for processing
        UpdatePersonRequest request = new UpdatePersonRequest(arg0, arg1);
        producerTemplate.sendBodyAndHeader("direct:updatePerson", request, "operation", "updatePerson");
    }

    @Override
    public Person getPerson(String arg0) throws NoSuchPersonException {
        System.out.println("getPerson called for: " + arg0);
        
        // Send to Camel route and get response
        Person result = producerTemplate.requestBodyAndHeader("direct:getPerson", arg0, "operation", "getPerson", Person.class);
        return result != null ? result : createDefaultPerson(arg0);
    }

    @Override
    public void deletePerson(String arg0) throws NoSuchPersonException {
        System.out.println("deletePerson called for: " + arg0);
        
        // Send to Camel route for processing
        producerTemplate.sendBodyAndHeader("direct:deletePerson", arg0, "operation", "deletePerson");
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