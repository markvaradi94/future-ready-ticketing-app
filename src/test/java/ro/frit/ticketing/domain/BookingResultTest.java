package ro.frit.ticketing.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test (no Spring): checks the messages from {@link BookingResult#describe()}.
 */
class BookingResultTest {

    @Test
    void describesConfirmed() {
        Booking booking = Booking.builder()
                .id("b1")
                .eventId("e1")
                .customerEmail("ana@mail.ro")
                .seats(2)
                .bookedAt(LocalDateTime.of(2026, 10, 12, 10, 0))
                .build();

        assertEquals("Event e1 CONFIRMED", new BookingResult.Confirmed(booking).describe());
    }

    @Test
    void describesSoldOut() {
        assertEquals("Event e1 SOLD OUT (1 seats available)", new BookingResult.SoldOut("e1", 1).describe());
    }

    @Test
    void describesUnknownEvent() {
        assertEquals("Event e9 NOT FOUND", new BookingResult.UnknownEvent("e9").describe());
    }

    @Test
    void describesAlreadyStarted() {
        BookingResult result = new BookingResult.AlreadyStarted("e1", LocalDateTime.of(2026, 10, 1, 19, 0));

        assertEquals("Event e1 HAS ALREADY STARTED", result.describe());
    }
}
