package ro.frit.gateway;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.caffeine.Bucket4jCaffeine;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.Bucket4jFilterFunctions.rateLimit;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

/**
 * The gateway's routes, in the Java DSL of Gateway Server WebMVC (not the WebFlux {@code RouteLocatorBuilder}).
 */
@Configuration
public class Routes {

    // In-memory token buckets, one per client key. In memory means per instance: two gateway instances allow
    // twice the rate.
    @Bean
    AsyncProxyManager<String> rateLimitBuckets() {
        return Bucket4jCaffeine.<String>builderFor(Caffeine.newBuilder().maximumSize(100_000)).build().asAsync();
    }

    @Bean
    RouterFunction<ServerResponse> ticketingRoute(GatewayProperties properties) {
        return route("ticketing")
                .route(path("/events/**"), http())
                .before(uri(properties.ticketingUri()))
                .filter(perClientRateLimit(properties.rateLimit()))
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> notificationsRoute(GatewayProperties properties) {
        return route("notifications")
                .route(path("/notifications/**"), http())
                .before(uri(properties.notificationsUri()))
                .filter(perClientRateLimit(properties.rateLimit()))
                .build();
    }

    // Over the limit: 429, and when to try again. A token comes back every period / capacity, in whole seconds.
    private static HandlerFilterFunction<ServerResponse, ServerResponse> perClientRateLimit(GatewayProperties.RateLimit limit) {
        HandlerFilterFunction<ServerResponse, ServerResponse> rateLimit = rateLimit(config -> config
                .setCapacity(limit.capacity())
                .setPeriod(limit.period())
                .setKeyResolver(Routes::clientIp));
        String retryAfter = String.valueOf(Math.max(1, Math.ceilDiv(limit.period().toSeconds(), limit.capacity())));
        return (request, next) -> {
            ServerResponse response = rateLimit.filter(request, next);
            if (!response.statusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
                return response;
            }
            // The filter's 429 is already built, its headers read-only: a copy with one header more.
            return ServerResponse.from(response).header(HttpHeaders.RETRY_AFTER, retryAfter).build();
        };
    }

    // Who "a client" is: its IP address. Behind a proxy every client has the proxy's address; then the key would
    // come from a trusted X-Forwarded-For header, or from a login.
    private static String clientIp(ServerRequest request) {
        return request.remoteAddress()
                .map(address -> address.getAddress().getHostAddress())
                .orElse("unknown");
    }
}
