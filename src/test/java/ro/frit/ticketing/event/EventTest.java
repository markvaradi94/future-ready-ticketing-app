package ro.frit.ticketing.event;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test (no Spring): {@link Event#availableSeats()}, the {@code toBuilder} copy, and the constraints on the
 * entity, checked with a Jakarta {@link Validator}.
 */
class EventTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private final Event event = Event.builder()
            .id("e1")
            .name("Concert")
            .venue(new Venue("Arena", "Cluj"))
            .startsAt(LocalDateTime.of(2026, 10, 20, 19, 0))
            .capacity(100)
            .bookedSeats(98)
            .build();

    @Test
    void availableSeatsIsCapacityMinusBookedSeats() {
        assertEquals(2, event.availableSeats());
    }

    @Test
    void toBuilderCopiesWithoutChangingTheOriginal() {
        Event copy = event.toBuilder().bookedSeats(99).build();

        assertNotSame(event, copy);
        assertEquals("e1", copy.getId());
        assertEquals(99, copy.getBookedSeats());
        assertEquals(98, event.getBookedSeats());
        assertEquals(event, copy);
    }

    @Test
    void validEventHasNoViolations() {
        assertTrue(validator.validate(event).isEmpty());
    }

    @Test
    void blankNameIsInvalid() {
        assertEquals(Set.of("name"), violatedProperties(event.toBuilder().name(" ").build()));
    }

    @Test
    void missingStartIsInvalid() {
        assertEquals(Set.of("startsAt"), violatedProperties(event.toBuilder().startsAt(null).build()));
    }

    @Test
    void zeroCapacityIsInvalid() {
        assertEquals(Set.of("capacity"), violatedProperties(event.toBuilder().capacity(0).bookedSeats(0).build()));
    }

    @Test
    void negativeBookedSeatsIsInvalid() {
        assertEquals(Set.of("bookedSeats"), violatedProperties(event.toBuilder().bookedSeats(-1).build()));
    }

    @Test
    void theVenueIsValidatedToo() {
        assertEquals(Set.of("venue.city"), violatedProperties(event.toBuilder().venue(new Venue("Arena", "")).build()));
    }

    private Set<String> violatedProperties(Event invalid) {
        return validator.validate(invalid).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
