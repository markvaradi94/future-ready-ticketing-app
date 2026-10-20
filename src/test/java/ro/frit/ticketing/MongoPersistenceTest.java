package ro.frit.ticketing;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.config.MongoConfig;
import ro.frit.ticketing.config.TestcontainersConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A persistence test: the MongoDB part of Spring only ({@code @DataMongoTest}), a MongoDB in Docker, and the same
 * validation before saving as the application has. Add the services under test with {@code @Import}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataMongoTest
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
@Import({TestcontainersConfiguration.class, MongoConfig.class})
public @interface MongoPersistenceTest {
}
