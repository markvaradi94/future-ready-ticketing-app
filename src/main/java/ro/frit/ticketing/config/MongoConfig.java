package ro.frit.ticketing.config;

import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.mapping.event.ValidatingEntityCallback;

/**
 * Checks the Jakarta Bean Validation constraints of an entity before Spring Data saves it. An invalid entity throws a
 * {@code ConstraintViolationException}, and nothing is written.
 */
@Configuration
public class MongoConfig {

    @Bean
    public ValidatingEntityCallback validatingEntityCallback(Validator validator) {
        return new ValidatingEntityCallback(validator);
    }
}
