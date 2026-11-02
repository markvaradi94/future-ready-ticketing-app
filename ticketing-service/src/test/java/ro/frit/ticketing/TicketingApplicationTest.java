package ro.frit.ticketing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.config.TestcontainersConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class TicketingApplicationTest {

    @Test
    void contextLoads() {}

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Test
    void healthShowsMongoDbAndRabbitMqUp() {
        assertEquals(Status.UP, healthEndpoint.healthForPath("mongo").getStatus());
        assertEquals(Status.UP, healthEndpoint.healthForPath("rabbit").getStatus());
        assertEquals(Status.UP, healthEndpoint.health().getStatus());
    }
}