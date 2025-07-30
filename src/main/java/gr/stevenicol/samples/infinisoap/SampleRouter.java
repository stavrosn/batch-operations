package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import samples.stevenicol.gr.soap.sampleservice.*;
import java.time.LocalDateTime;

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

        // Route for writePersonsToFile operation - JDBC streaming with CSV output
        from("direct:writePersonsToFile")
                .log("🚀 Starting writePersonsToFile operation with JDBC streaming")
                .setProperty("exportStartTime", constant(System.currentTimeMillis()))
                .setProperty("exportFileName", simple("persons_export_${date:now:yyyyMMdd_HHmmss}.csv"))
                .log("📝 Exporting persons to CSV file: ${exchangeProperty.exportFileName}")
                
                // Execute streaming SQL query using JDBC component
                .setBody(constant("SELECT p.name, p.contact_type, a.street, a.city, a.postal_code " +
                        "FROM persons p LEFT JOIN addresses a ON p.address_id = a.id ORDER BY p.id"))
                .to("jdbc:dataSource?outputType=StreamList&useIterator=true")
                .log("📊 Starting to stream SQL results to CSV")
                
                // Configure CSV format
                .marshal().csv()
                .log("✅ Marshalled streaming results to CSV format")
                
                // Write CSV data to file with streaming
                .to("file:exports?fileName=${exchangeProperty.exportFileName}&fileExist=Override")
                
                // Log completion statistics
                .process(exchange -> {
                    long startTime = (Long) exchange.getProperty("exportStartTime");
                    long totalTime = System.currentTimeMillis() - startTime;
                    String fileName = (String) exchange.getProperty("exportFileName");
                    
                    log.info("✅ CSV export completed successfully!");
                    log.info("📊 Export Statistics:");
                    log.info("   📄 File: exports/{}", fileName);
                    log.info("   ⏱️ Total time: {:.2f} seconds", totalTime / 1000.0);
                    log.info("   💾 Format: CSV with headers");
                    log.info("   🚀 Method: JDBC streaming with outputType=StreamList");
                    
                    // Return success
                    exchange.getIn().setBody(true);
                })
                .onException(Exception.class)
                    .handled(true)
                    .log("❌ Error in JDBC streaming export: ${exception.message}")
                    .setBody(constant(false))
                .end();

        // Route for writePersonsToCache operation - JDBC streaming to Infinispan cache
        from("direct:writePersonsToCache")
                .log("🚀 Starting writePersonsToCache operation - JDBC streaming to Infinispan")
                .setProperty("cacheStartTime", constant(System.currentTimeMillis()))
                .setProperty("cacheKey", simple("persons-db-export-${date:now:yyyyMMdd_HHmmss}"))
                .log("📦 Caching persons data to Infinispan with key: ${exchangeProperty.cacheKey}")
                
                // Execute streaming SQL query using JDBC component
                .setBody(constant("SELECT p.name, p.contact_type, a.street, a.city, a.postal_code " +
                        "FROM persons p LEFT JOIN addresses a ON p.address_id = a.id ORDER BY p.id"))
                .to("jdbc:dataSource?outputType=StreamList&useIterator=true")
                .log("📊 Starting to stream SQL results to CSV for caching")
                
                // Configure CSV format - same as file export
                .marshal().csv()
                .log("✅ Marshalled streaming results to CSV format")
                
                // Prepare CSV data for streaming cache
                .process(exchange -> {
                    String cacheKey = (String) exchange.getProperty("cacheKey");
                    String csvData = exchange.getIn().getBody(String.class);
                    long dataSize = csvData.length();
                    
                    try {
                        // Set headers for service implementation to use StreamingCacheService
                        exchange.getIn().setHeader("StreamingCacheKey", cacheKey);
                        exchange.getIn().setHeader("StreamingCacheData", csvData);
                        exchange.getIn().setHeader("CacheDataSize", dataSize);
                        exchange.getIn().setHeader("CacheDataFormat", "CSV");
                        
                        log.info("🚀 Prepared CSV data for streaming cache: key={}, size={} chars", 
                                cacheKey, dataSize);
                        
                    } catch (Exception e) {
                        log.error("❌ Error preparing CSV data for streaming cache: {}", e.getMessage(), e);
                        throw new RuntimeException("Cache preparation error", e);
                    }
                })
                
                // Signal completion statistics  
                .process(exchange -> {
                    long startTime = (Long) exchange.getProperty("cacheStartTime");
                    long totalTime = System.currentTimeMillis() - startTime;
                    String cacheKey = (String) exchange.getProperty("cacheKey");
                    long dataSize = (Long) exchange.getIn().getHeader("CacheDataSize");
                    
                    log.info("✅ Database to cache CSV streaming completed!");
                    log.info("📊 Cache Statistics:");
                    log.info("   🔑 Cache Key: {}", cacheKey);
                    log.info("   📏 Data size: {:.2f} MB ({} chars)", dataSize / (1024.0 * 1024.0), dataSize);
                    log.info("   ⏱️ Total time: {:.2f} seconds", totalTime / 1000.0);
                    log.info("   💾 Format: CSV (same as file export)");
                    log.info("   🚀 Method: JDBC streaming → CSV marshalling → Infinispan chunked storage");
                    
                    // The actual caching will be done by the service implementation
                    // We return success here, and the service layer handles the StreamingCacheService
                    exchange.getIn().setBody(true);
                })
                .onException(Exception.class)
                    .handled(true)
                    .log("❌ Error in database to cache CSV streaming: ${exception.message}")
                    .setBody(constant(false))
                .end();
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
    
    private String escapeJson(Object value) {
        if (value == null) return "";
        return value.toString().replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
