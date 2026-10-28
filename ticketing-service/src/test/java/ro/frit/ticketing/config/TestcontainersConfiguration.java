package ro.frit.ticketing.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@TestConfiguration
@ImportTestcontainers
public class TestcontainersConfiguration {

    @ServiceConnection
    // MongoDB 8.0 refuses to start on Linux kernel 6.19 and newer (SERVER-121912), as on GitHub's newer runners;
    // this setting lets it start there. Without it, the build fails in CI only.
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0")
            .withEnv("GLIBC_TUNABLES", "glibc.pthread.rseq=1");

    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:4-management");
}
