package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import samples.stevenicol.gr.soap.sampleservice.*;

@ApplicationScoped
public class SampleRouter extends RouteBuilder {

    private final ObjectFactory objectFactory = new ObjectFactory();

    @Override
    public void configure() throws Exception {

        // Route for getPersons operation
        from("direct:getPersons")
                .log("Processing getPersons request")
                .process(exchange -> {
                    Persons persons = objectFactory.createPersons();
                    persons.getPersons().add(createSamplePerson("John Doe", "123 Main St", "New York", "10001", ContactType.PERSONAL));
                    persons.getPersons().add(createSamplePerson("Jane Smith", "456 Oak Ave", "Boston", "02101", ContactType.WORK));
                    exchange.getIn().setBody(persons);
                });

        // Route for getPerson operation
        from("direct:getPerson")
                .log("Processing getPerson request for: ${body}")
                .process(exchange -> {
                    String personName = exchange.getIn().getBody(String.class);
                    Person person = createSamplePerson(personName != null ? personName : "Unknown", 
                                                     "123 Main St", "New York", "10001", ContactType.PERSONAL);
                    exchange.getIn().setBody(person);
                });

        // Route for addPerson operation
        from("direct:addPerson")
                .log("Processing addPerson request")
                .process(exchange -> {
                    Person person = exchange.getIn().getBody(Person.class);
                    if (person != null) {
                        log.info("Adding person: " + person.getName());
                        // Here you can add your business logic (database save, etc.)
                    }
                });

        // Route for updatePerson operation
        from("direct:updatePerson")
                .log("Processing updatePerson request")
                .process(exchange -> {
                    SampleServiceImpl.UpdatePersonRequest request = exchange.getIn().getBody(SampleServiceImpl.UpdatePersonRequest.class);
                    if (request != null) {
                        log.info("Updating person: " + request.getPersonName() + " with new data: " + 
                               (request.getPerson() != null ? request.getPerson().getName() : "null"));
                        // Here you can add your business logic (database update, etc.)
                    }
                });

        // Route for deletePerson operation
        from("direct:deletePerson")
                .log("Processing deletePerson request for: ${body}")
                .process(exchange -> {
                    String personName = exchange.getIn().getBody(String.class);
                    log.info("Deleting person: " + personName);
                    // Here you can add your business logic (database delete, etc.)
                });

        // Route for large persons dataset (used by streaming cache service)
        from("direct:getLargePersons")
                .log("Processing getLargePersons request - generating large dataset")
                .process(exchange -> {
                    Persons persons = objectFactory.createPersons();
                    
                    // Generate 10,000 sample persons for large dataset testing
                    for (int i = 1; i <= 10000; i++) {
                        String name = "Person " + i;
                        String street = "Street " + i + " Avenue";
                        String city = "City " + (i % 100);
                        String postalCode = String.format("%05d", i);
                        ContactType type = i % 2 == 0 ? ContactType.WORK : ContactType.PERSONAL;
                        
                        Person person = createSamplePerson(name, street, city, postalCode, type);
                        persons.getPersons().add(person);
                        
                        // Log progress every 1000 records
                        if (i % 1000 == 0) {
                            log.info("🏗️ Generated " + i + " persons in router...");
                        }
                    }
                    
                    log.info("✅ Large dataset generated in router: " + persons.getPersons().size() + " persons");
                    exchange.getIn().setBody(persons);
                });
    }

    private Person createSamplePerson(String name, String street, String city, String postalCode, ContactType type) {
        Person person = objectFactory.createPerson();
        person.setName(name);
        person.setType(type);
        
        Address address = objectFactory.createAddress();
        address.setStreet(street);
        address.setCity(city);
        address.setPostalCode(postalCode);
        person.setAddress(address);
        
        return person;
    }
}
