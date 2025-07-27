# SOAP Web Services with Centralized Interceptor Pattern

## Project Overview

This project demonstrates a robust enterprise pattern for SOAP web services using **Quarkus CXF** with **Apache Camel** integration and **centralized cross-cutting concerns** (auditing and security).

## Architecture

### Technologies Used
- **Quarkus CXF**: SOAP web service implementation
- **Apache Camel**: Integration routing and patterns
- **JAX-WS**: Web service annotations and contracts
- **CDI**: Dependency injection and interceptors

### Design Pattern
- **Contract-first approach**: WSDL → Generated interfaces → Service implementation
- **Interceptor pattern**: Centralized cross-cutting concerns for auditing and security
- **Library-ready design**: Reusable components for multiple SOAP services

## Key Components

### 1. Core Service Files

#### `SampleService.wsdl`
- WSDL contract defining 5 operations: `addPerson`, `getPersons`, `getPerson`, `updatePerson`, `deletePerson`
- Complex types: `Person`, `Address`, `ContactType` enum
- Exception handling: `NoSuchPersonException`

#### `SampleService.java` (Generated)
- Auto-generated interface from WSDL using CXF wsdl2java
- JAX-WS annotations for web service contract

#### `SampleServiceImpl.java`
- Service implementation extending `AbstractInterceptedService`
- Routes all operations through centralized interceptor
- Provides fallback responses when routes return null

#### `SampleRouter.java`
- Camel routes for business logic (`direct:addPerson`, `direct:getPersons`, etc.)
- Sample data creation and processing logic

### 2. Interceptor Framework

#### `ServiceRequest.java`
- Wrapper class for service requests with metadata
- Contains operation name, body, target route, user context, audit data
- Timestamp tracking for performance monitoring

#### `AbstractInterceptedService.java` ⭐
- **Core enforcement mechanism** - All SOAP services must extend this class
- `executeWithInterceptor()` method ensures all operations go through interceptor
- Handles both void and return-type operations
- Provides user/session context abstraction

#### `InterceptorRouter.java` ⭐
- **Centralized cross-cutting concerns** implementation
- Route: `direct:soap-interceptor` → `direct:audit-incoming` → `direct:security-check` → target route → `direct:audit-outgoing`
- Audit logging with timestamps, user tracking, processing time
- Security validation and authorization checks
- **Library-ready**: No dependencies on business logic

### 3. Alternative Enforcement (CDI-based)

#### `@Intercepted` Annotation
- Method-level annotation to mark intercepted operations
- Declarative approach: specify target route and operation name

#### `ServiceInterceptor.java`
- CDI interceptor that automatically processes `@Intercepted` methods
- Alternative to abstract base class approach
- Requires `beans.xml` configuration

#### `SampleServiceAnnotatedImpl.java`
- Example implementation using annotation-based approach
- Methods annotated with `@Intercepted` are automatically routed through interceptor

## Implementation Patterns

### Pattern 1: Abstract Base Class (Recommended)
```java
public class SampleServiceImpl extends AbstractInterceptedService implements SampleService {
    @Override
    public Persons getPersons() {
        return executeWithInterceptor("getPersons", null, "direct:getPersons", Persons.class);
    }
    
    @Override 
    public void addPerson(Person person) {
        executeWithInterceptor("addPerson", person, "direct:addPerson");
    }
}
```

**Benefits:**
- ✅ Compile-time enforcement
- ✅ Library-ready design
- ✅ Type-safe operations
- ✅ IDE support and refactoring

### Pattern 2: Annotation-based (Alternative)
```java
@Intercepted(targetRoute = "direct:getPersons", operation = "getPersons")
public Persons getPersons() {
    // Business logic - interceptor runs automatically
}
```

**Benefits:**
- ✅ Transparent to business logic
- ✅ Declarative approach
- ❌ Requires CDI configuration
- ❌ Runtime enforcement only

## Cross-Cutting Concerns

### Auditing Features
- **Incoming requests**: Operation, user, session, timestamp logging
- **Performance monitoring**: Processing time measurement
- **Client tracking**: IP address and session management
- **Outgoing responses**: Result logging and audit trail

### Security Features
- **Authorization checks**: User validation per operation
- **Access control**: Operation-level security rules
- **Security warnings**: Unauthorized access attempt logging
- **Context propagation**: User/session context through request lifecycle

## Library Distribution Strategy

### Core Library Components (Reusable)
```
interceptor-library/
├── ServiceRequest.java
├── AbstractInterceptedService.java
├── InterceptorRouter.java
├── @Intercepted.java
└── ServiceInterceptor.java
```

### Business Service Components (Project-specific)
```
business-service/
├── SampleService.wsdl
├── SampleServiceImpl.java (extends AbstractInterceptedService)
└── SampleRouter.java (business logic routes)
```

### Maven Dependency Structure
```xml
<!-- In business service project -->
<dependency>
    <groupId>your.company</groupId>
    <artifactId>soap-interceptor-library</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage for New SOAP Services

### Step 1: Create WSDL Contract
- Define operations, types, and exceptions in WSDL

### Step 2: Generate Service Interface
- Use CXF wsdl2java to generate JAX-WS interface

### Step 3: Implement Service
```java
@WebService(...)
@ApplicationScoped
public class YourServiceImpl extends AbstractInterceptedService implements YourService {
    
    @Override
    public YourResponse yourOperation(YourRequest request) {
        return executeWithInterceptor("yourOperation", request, "direct:yourOperation", YourResponse.class);
    }
}
```

### Step 4: Create Business Logic Routes
```java
@ApplicationScoped
public class YourRouter extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:yourOperation")
            .log("Processing yourOperation")
            .process(exchange -> {
                // Your business logic here
            });
    }
}
```

## Configuration

### Maven Dependencies
```xml
<dependency>
    <groupId>org.apache.camel.quarkus</groupId>
    <artifactId>camel-quarkus-cxf-soap</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.camel.quarkus</groupId>
    <artifactId>camel-quarkus-direct</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.camel.quarkus</groupId>
    <artifactId>camel-quarkus-log</artifactId>
</dependency>
```

### SOAP Endpoint Configuration
- Service endpoint: `http://localhost:8080/cxf/services/person`
- WSDL location: `http://localhost:8080/cxf/services/person?wsdl`

## Benefits of This Architecture

### For Development Teams
- **Consistent patterns**: All SOAP services follow same interceptor pattern
- **Reduced boilerplate**: Abstract base class eliminates repetitive interceptor code
- **Type safety**: Compile-time enforcement prevents bypassing interceptors
- **Easy testing**: Clean separation between cross-cutting concerns and business logic

### For Enterprise Operations
- **Centralized auditing**: All SOAP operations automatically logged
- **Security compliance**: Consistent authorization checks across services
- **Performance monitoring**: Built-in processing time measurement
- **Troubleshooting**: Comprehensive request/response logging

### For Maintenance
- **Library distribution**: Interceptor framework in separate JAR
- **Version management**: Update cross-cutting concerns independently
- **Backward compatibility**: New interceptor features don't break existing services
- **Code reuse**: Same patterns across multiple microservices

## Testing

### Unit Testing

#### `SampleServiceTest.java`
Comprehensive test suite for the `getPersons` SOAP web service method that validates the complete interceptor integration flow.

**Test Coverage:**
- ✅ **Full Integration Flow**: Tests the complete chain from service → interceptor → router → response
- ✅ **Interceptor Functionality**: Validates audit logging, security checks, and performance monitoring
- ✅ **Data Validation**: Verifies `Persons` object structure and content
- ✅ **Sample Data Verification**: Checks for expected "John Doe" and "Jane Smith" test data
- ✅ **Address Validation**: Tests complete `Address` object properties
- ✅ **Contact Type Validation**: Verifies `ContactType` enum values
- ✅ **Fallback Mechanism**: Tests error handling when routes return null

**Test Methods:**

1. **`testGetPersons()`** - Main integration test
   - Calls `sampleService.getPersons()` through the full interceptor chain
   - Validates response structure and sample data
   - Confirms audit and security logging functionality
   - Verifies processing time measurement

2. **`testGetPersonsWithFallback()`** - Resilience test
   - Tests the fallback mechanism when Camel routes return null
   - Ensures service always returns valid data for reliability

**Integration Flow Tested:**
```
SampleServiceImpl → AbstractInterceptedService → direct:soap-interceptor → 
InterceptorRouter → direct:audit-incoming → direct:security-check → 
direct:getPersons → SampleRouter → Response
```

#### Running the Tests

**Option 1: Run Single Test Class**
```bash
mvn test -Dtest=SampleServiceTest
```

**Option 2: Run All Tests**
```bash
mvn test
```

**Option 3: Run with Quarkus Test Mode (Recommended)**
```bash
mvn quarkus:test
```
*Starts Quarkus in test mode with hot reload - ideal for development*

**Option 4: Run Specific Test Method**
```bash
mvn test -Dtest=SampleServiceTest#testGetPersons
```

**Option 5: From IDE**
- Right-click on `SampleServiceTest.java` → "Run Tests"
- Or right-click on individual test methods

#### Expected Test Output

When running the tests, you'll see:

```
=== INTERCEPTOR: Processing getPersons operation ===
AUDIT-IN: Operation=getPersons, User=demo-user, Session=session-1234567890...
SECURITY: Validating access for operation=getPersons
AUDIT-OUT: Operation completed successfully, Response=...
AUDIT: Processing time for operation: 45ms
✅ getPersons test completed successfully
📊 Returned 2 persons
🔄 Interceptor integration verified (audit and security logging should appear in console)
```

**What the Test Validates:**
- 🔍 **Interceptor Integration**: Confirms audit and security logs appear
- ⏱️ **Performance Monitoring**: Verifies processing time measurement
- 📊 **Data Quality**: Validates business logic execution
- 🛡️ **Security Validation**: Tests authorization checks
- 🔄 **End-to-End Flow**: Complete SOAP service integration

### Running the Service
```bash
mvn quarkus:dev
```

### Manual SOAP Testing
```bash
# Test getPersons operation
curl -X POST http://localhost:8080/cxf/services/person \
  -H "Content-Type: text/xml" \
  -d '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">...</soap:Envelope>'
```

## Future Enhancements

### Planned Features
- **Metrics collection**: Prometheus/Micrometer integration
- **Distributed tracing**: OpenTelemetry support
- **Caching layer**: Response caching for read operations
- **Rate limiting**: Request throttling per user/operation
- **Circuit breaker**: Fault tolerance patterns

### Library Evolution
- **Configuration externalization**: Properties-based interceptor configuration
- **Custom interceptor plugins**: Pluggable interceptor implementations
- **Multi-tenant support**: Tenant-aware auditing and security
- **Event sourcing**: Audit event publishing to message queues

## Conclusion

This implementation provides a robust, enterprise-ready foundation for SOAP web services with:

✅ **Enforced consistency** through abstract base class pattern  
✅ **Comprehensive auditing** and security cross-cutting concerns  
✅ **Library-ready architecture** for reuse across projects  
✅ **Clean separation** between infrastructure and business logic  
✅ **Scalable design** for multiple SOAP services  

The pattern demonstrated here ensures that all future SOAP services will automatically inherit proper auditing, security, and monitoring capabilities while maintaining clean, maintainable code.