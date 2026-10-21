package ro.frit.ticketing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.config.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class TicketingApplicationTest {

    @Test
    void contextLoads() {}
}