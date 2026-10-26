package ro.frit.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ro.frit.notification.config.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NotificationApplicationTest {

    @Test
    void contextLoads() {}
}
