package ro.frit.ticketing.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TicketOfficeTest {

    @Test
    void unknownEventIsReported() {
        TicketOffice office = new TicketOffice(List.of(event("event-1", 100, 0)));

        BookingResult result = office.book(new BookingRequest("event-20", "beni@nu.ro", 2));

        assertEquals(new BookingResult.UnknownEvent("event-20"), result);
    }

    @Test
    void soldOutIsReported() {
        TicketOffice office = new TicketOffice(List.of(event("event-1", 10, 8)));

        BookingResult result = office.book(new BookingRequest("event-1", "beni@nu.ro", 5));

        assertEquals(new BookingResult.SoldOut("event-1", 2), result);
    }

    @Test
    void bookingIsSuccessful() {
        TicketOffice office = new TicketOffice(List.of(event("e1", 100, 10)));

        BookingResult result = office.book(new BookingRequest("e1", "asda@afdsa.com", 2));

        BookingResult.Confirmed confirmed = assertInstanceOf(BookingResult.Confirmed.class, result);
        assertEquals(2, confirmed.booking().getSeats());
    }

    private static Event event(String id, int capacity, int bookedSeats) {
        return Event.builder()
                .id(id)
                .capacity(capacity)
                .bookedSeats(bookedSeats)
                .startsAt(LocalDateTime.now().plusDays(7))
                .name("Event " + id)
                .venue(new Venue("Arena", "Cluj"))
                .build();
    }
}