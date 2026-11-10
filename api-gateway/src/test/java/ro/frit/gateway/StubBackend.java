package ro.frit.gateway;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * A tiny HTTP server standing in for a service, from the JDK, no library. It answers every request with
 * {@code "<name> <method> <path>"}, so a test can see where the request went.
 */
final class StubBackend {

    private final HttpServer server;
    private volatile String lastAuthorization;

    StubBackend(String name) {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", exchange -> {
            lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            String answer = name + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI() + " "
                    + exchange.getRequestHeaders().getFirst(CorrelationIdFilter.HEADER);
            byte[] body = answer.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    String uri() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    void stop() {
        server.stop(0);
    }

    String lastAuthorization() {
        return lastAuthorization;
    }
}
