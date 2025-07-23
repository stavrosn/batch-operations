# batch-operations

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/batch-operations-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- Camel Log ([guide](https://camel.apache.org/camel-quarkus/latest/reference/extensions/log.html)): Prints data form the routed message (such as body and headers) to the logger
- Camel CXF ([guide](https://camel.apache.org/camel-quarkus/latest/reference/extensions/cxf-soap.html)): Expose SOAP WebServices using Apache CXF or connect to external WebServices using CXF WS client
- JDBC Driver - Microsoft SQL Server ([guide](https://quarkus.io/guides/datasource)): Connect to the Microsoft SQL Server database via JDBC
- Agroal - DB connection pool ([guide](https://quarkus.io/guides/datasource)): JDBC Datasources and connection pooling
- Camel Infinispan ([guide](https://camel.apache.org/camel-quarkus/latest/reference/extensions/infinispan.html)): Read and write from/to Infinispan distributed key/value store and data grid
- Camel Bean ([guide](https://camel.apache.org/camel-quarkus/latest/reference/extensions/bean.html)): Invoke methods of Java beans
- Camel JDBC ([guide](https://camel.apache.org/camel-quarkus/latest/reference/extensions/jdbc.html)): Access databases through SQL and JDBC
