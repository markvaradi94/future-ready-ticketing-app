package ro.frit.ticketing.booking;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test (no Spring): the constraints on the {@link Booking} entity, checked with a Jakarta {@link Validator}.
 */
class BookingTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private final Booking booking = Booking.builder()
            .id("b1")
            .eventId("e1")
            .customerEmail("ana@mail.ro")
            .seats(2)
            .bookedAt(LocalDateTime.of(2030, 1, 5, 10, 30))
            .build();

    @Test
    void validBookingHasNoViolations() {
        assertTrue(validator.validate(booking).isEmpty());
    }

    @Test
    void invalidEmailIsInvalid() {
        assertEquals(Set.of("customerEmail"), violatedProperties(booking.toBuilder().customerEmail("not-an-email").build()));
    }

    @Test
    void zeroSeatsIsInvalid() {
        assertEquals(Set.of("seats"), violatedProperties(booking.toBuilder().seats(0).build()));
    }

    @Test
    void missingEventIdIsInvalid() {
        assertEquals(Set.of("eventId"), violatedProperties(booking.toBuilder().eventId(null).build()));
    }

    private Set<String> violatedProperties(Booking invalid) {
        return validator.validate(invalid).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
