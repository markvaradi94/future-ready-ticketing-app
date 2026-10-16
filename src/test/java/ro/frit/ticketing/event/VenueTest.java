package ro.frit.ticketing.event;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test (no Spring): the constraints on the {@link Venue} record, checked with a Jakarta {@link Validator}.
 */
class VenueTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validVenueHasNoViolations() {
        assertTrue(validator.validate(new Venue("Arena", "Cluj")).isEmpty());
    }

    @Test
    void blankNameIsInvalid() {
        assertEquals(Set.of("name"), violatedProperties(new Venue(" ", "Cluj")));
    }

    @Test
    void nullNameIsInvalid() {
        assertEquals(Set.of("name"), violatedProperties(new Venue(null, "Cluj")));
    }

    @Test
    void blankCityIsInvalid() {
        assertEquals(Set.of("city"), violatedProperties(new Venue("Arena", " ")));
    }

    @Test
    void nullCityIsInvalid() {
        assertEquals(Set.of("city"), violatedProperties(new Venue("Arena", null)));
    }

    private Set<String> violatedProperties(Venue invalid) {
        return validator.validate(invalid).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
