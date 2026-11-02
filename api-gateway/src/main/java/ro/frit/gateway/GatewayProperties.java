package ro.frit.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * Where the services are. Only this configuration changes between a local run, Compose and Cloud Run.
 */
@ConfigurationProperties("gateway")
public record GatewayProperties(URI ticketingUri, URI notificationsUri, RateLimit rateLimit) {

    // How many requests a client may send per period, across all routes.
    public record RateLimit(long capacity, Duration period) {
    }
}
