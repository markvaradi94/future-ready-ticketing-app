package ro.frit.gateway;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test (the gateway on a random port, the service replaced by a {@link StubBackend}): a client gets three
 * requests per minute, then 429.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRateLimitTest {

    private static final StubBackend ticketing = new StubBackend("ticketing");

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void routeToTheStubs(DynamicPropertyRegistry registry) {
        registry.add("gateway.ticketing-uri", ticketing::uri);
        registry.add("gateway.notifications-uri", ticketing::uri);
        registry.add("gateway.rate-limit.capacity", () -> 3);
        registry.add("gateway.rate-limit.period", () -> "1m");
    }

    @AfterAll
    static void stopBackends() {
        ticketing.stop();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void aClientOverTheLimitGets429() throws Exception {
        assertEquals(200, get("/events").statusCode());
        assertEquals(200, get("/events/rock-cluj").statusCode());
        assertEquals(200, get("/events").statusCode());

        HttpResponse<String> overTheLimit = get("/events");
        assertEquals(429, overTheLimit.statusCode());
        assertEquals("0", overTheLimit.headers().firstValue("X-RateLimit-Remaining").orElseThrow());
        assertEquals("20", overTheLimit.headers().firstValue("Retry-After").orElseThrow());
    }
}
