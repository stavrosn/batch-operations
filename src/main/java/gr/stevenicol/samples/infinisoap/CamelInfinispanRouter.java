package gr.stevenicol.samples.infinisoap;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel routes for Infinispan cache operations.
 * Handles large string caching operations (125-150MB) efficiently.
 */
@ApplicationScoped
public class CamelInfinispanRouter extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Route for PUT operations - store data in cache with error handling
        from("direct:cache-put")
                .log("CACHE-PUT: Storing data with key=${header.CamelInfinispanKey}")
                .doTry()
                    .to("infinispan:large-string-cache?operation=PUT")
                    .log("CACHE-PUT: ✅ Successfully stored data with key=${header.CamelInfinispanKey}")
                    .setBody(constant("SUCCESS"))
                .doCatch(Exception.class)
                    .log("CACHE-PUT: ❌ Failed to store data with key=${header.CamelInfinispanKey}, error: ${exception.message}")
                    .setBody(constant("FAILED"))
                    .setHeader("CamelInfinispanError", simple("${exception.message}"))
                .end();

        // Route for GET operations - retrieve data from cache
        from("direct:cache-get")
                .log("CACHE-GET: Retrieving data with key=${header.CamelInfinispanKey}")
                .to("infinispan:large-string-cache?operation=GET")
                .choice()
                    .when(body().isNotNull())
                        .log("CACHE-GET: Found data for key=${header.CamelInfinispanKey}")
                    .otherwise()
                        .log("CACHE-GET: No data found for key=${header.CamelInfinispanKey}")
                .end();

        // Route for CONTAINSKEY operations - check if key exists
        from("direct:cache-containskey")
                .log("CACHE-CONTAINSKEY: Checking existence of key=${header.CamelInfinispanKey}")
                .to("infinispan:large-string-cache?operation=CONTAINSKEY")
                .log("CACHE-CONTAINSKEY: Key ${header.CamelInfinispanKey} exists=${body}");

        // Route for REMOVE operations - remove data from cache
        from("direct:cache-remove")
                .log("CACHE-REMOVE: Removing data with key=${header.CamelInfinispanKey}")
                .to("infinispan:large-string-cache?operation=REMOVE")
                .log("CACHE-REMOVE: Successfully removed data with key=${header.CamelInfinispanKey}");

        // Route for SIZE operations - get cache size
        from("direct:cache-size")
                .log("CACHE-SIZE: Getting cache size")
                .to("infinispan:large-string-cache?operation=SIZE")
                .log("CACHE-SIZE: Cache contains ${body} entries");

        // Route for CLEAR operations - clear entire cache
        from("direct:cache-clear")
                .log("CACHE-CLEAR: Clearing entire cache")
                .to("infinispan:large-string-cache?operation=CLEAR")
                .log("CACHE-CLEAR: Cache cleared successfully");

        // Route for PUTASYNC operations - asynchronous put with callback handling
        from("direct:cache-put-async")
                .log("CACHE-PUT-ASYNC: Storing data asynchronously with key=${header.CamelInfinispanKey}")
                .doTry()
                    .to("infinispan:large-string-cache?operation=PUTASYNC")
                    .log("CACHE-PUT-ASYNC: ✅ Successfully initiated async storage for key=${header.CamelInfinispanKey}")
                    .setBody(constant("ASYNC_STARTED"))
                .doCatch(Exception.class)
                    .log("CACHE-PUT-ASYNC: ❌ Failed to initiate async storage for key=${header.CamelInfinispanKey}, error: ${exception.message}")
                    .setBody(constant("ASYNC_FAILED"))
                    .setHeader("CamelInfinispanError", simple("${exception.message}"))
                .end();

        // Route for GETASYNC operations - asynchronous get
        from("direct:cache-get-async")
                .log("CACHE-GET-ASYNC: Retrieving data asynchronously with key=${header.CamelInfinispanKey}")
                .to("infinispan:large-string-cache?operation=GETASYNC")
                .choice()
                    .when(body().isNotNull())
                        .log("CACHE-GET-ASYNC: Found data for key=${header.CamelInfinispanKey}")
                    .otherwise()
                        .log("CACHE-GET-ASYNC: No data found for key=${header.CamelInfinispanKey}")
                .end();

        // Advanced route: Cache with TTL (Time To Live)
        from("direct:cache-put-ttl")
                .log("CACHE-PUT-TTL: Storing data with TTL for key=${header.CamelInfinispanKey}, TTL=${header.CamelInfinispanLifespan}ms")
                .to("infinispan:large-string-cache?operation=PUT")
                .log("CACHE-PUT-TTL: Successfully stored data with TTL for key=${header.CamelInfinispanKey}");

        // Advanced route: Bulk operations
        from("direct:cache-putall")
                .log("CACHE-PUTALL: Storing multiple entries")
                .to("infinispan:large-string-cache?operation=PUTALL")
                .log("CACHE-PUTALL: Successfully stored multiple entries");

        // Advanced route: Query cache (if needed for complex operations)
        from("direct:cache-query")
                .log("CACHE-QUERY: Executing query")
                .to("infinispan:large-string-cache?operation=QUERY")
                .log("CACHE-QUERY: Query executed, results: ${body}");
    }
}