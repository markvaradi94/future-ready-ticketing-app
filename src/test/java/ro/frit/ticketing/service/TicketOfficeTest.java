package ro.frit.ticketing.service;

import org.junit.jupiter.api.Test;
import ro.frit.ticketing.domain.Booking;
import ro.frit.ticketing.domain.BookingRequest;
import ro.frit.ticketing.domain.BookingResult;
import ro.frit.ticketing.domain.Event;
import ro.frit.ticketing.domain.Venue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketOfficeTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

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

    @Test
    void confirmedBookingIncreasesBookedSeats() {
        TicketOffice office = new TicketOffice(List.of(event("e1", 100, 10)));

        office.book(new BookingRequest("e1", "ana@mail.ro", 2));

        assertEquals(12, office.findEvent("e1").orElseThrow().getBookedSeats());
    }

    @Test
    void secondBookingAddsToBookedSeats() {
        TicketOffice office = new TicketOffice(List.of(event("e1", 100, 0)));

        office.book(new BookingRequest("e1", "ana@mail.ro", 2));
        office.book(new BookingRequest("e1", "ion@mail.ro", 3));

        assertEquals(5, office.findEvent("e1").orElseThrow().getBookedSeats());
    }

    @Test
    void requestForExactlyTheFreeSeatsIsConfirmed() {
        TicketOffice office = new TicketOffice(List.of(event("e1", 10, 8)));

        BookingResult result = office.book(new BookingRequest("e1", "ana@mail.ro", 2));

        assertInstanceOf(BookingResult.Confirmed.class, result);
        assertEquals(0, office.findEvent("e1").orElseThrow().availableSeats());
    }

    @Test
    void soldOutDoesNotChangeBookedSeats() {
        TicketOffice office = new TicketOffice(List.of(event("e1", 10, 8)));

        office.book(new BookingRequest("e1", "ana@mail.ro", 3));

        assertEquals(8, office.findEvent("e1").orElseThrow().getBookedSeats());
        assertTrue(office.bookedSeatsPerEvent().isEmpty());
    }

    @Test
    void duplicateEventIdsAreRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new TicketOffice(List.of(event("e1", 100, 0), event("e1", 50, 0))));

        assertTrue(exception.getMessage().contains("e1"));
    }

    @Test
    void startedEventIsRejected() {
        LocalDateTime startsAt = NOW.minusDays(1);
        TicketOffice office = new TicketOffice(List.of(event("e1", "Concert", "Cluj", startsAt, 100, 0)));

        BookingResult result = office.book(new BookingRequest("e1", "ana@mail.ro", 2));

        assertEquals(new BookingResult.AlreadyStarted("e1", startsAt), result);
        assertEquals(0, office.findEvent("e1").orElseThrow().getBookedSeats());
    }

    @Test
    void startedEventIsCheckedBeforeSeats() {
        LocalDateTime startsAt = NOW.minusDays(1);
        TicketOffice office = new TicketOffice(List.of(event("e1", "Concert", "Cluj", startsAt, 10, 10)));

        BookingResult result = office.book(new BookingRequest("e1", "ana@mail.ro", 2));

        assertEquals(new BookingResult.AlreadyStarted("e1", startsAt), result);
    }

    @Test
    void eventsWithFreeSeatsSkipsFullEvents() {
        TicketOffice office = new TicketOffice(List.of(event("e1", 10, 5), event("e2", 10, 10)));

        assertEquals(List.of("e1"), ids(office.eventsWithFreeSeats()));
    }

    @Test
    void bookedSeatsPerEventSumsBookings() {
        TicketOffice office = new TicketOffice(List.of(event("e1", 100, 0), event("e2", 100, 0), event("e3", 100, 0)));
        office.book(new BookingRequest("e1", "ana@mail.ro", 2));
        office.book(new BookingRequest("e1", "ion@mail.ro", 3));
        office.book(new BookingRequest("e2", "ana@mail.ro", 1));

        assertEquals(Map.of("e1", 5, "e2", 1), office.bookedSeatsPerEvent());
    }

    @Test
    void topEventsAreSortedByBookedSeats() {
        TicketOffice office = new TicketOffice(List.of(
                event("e1", "Concert", "Cluj", NOW.plusDays(1), 100, 5),
                event("e2", "Festival", "Cluj", NOW.plusDays(1), 100, 8),
                event("e3", "Play", "Cluj", NOW.plusDays(1), 100, 0)));

        assertEquals(List.of("e2", "e1"), ids(office.topEventsByBookedSeats(2)));
    }

    @Test
    void topEventsBreaksTiesByName() {
        TicketOffice office = new TicketOffice(List.of(
                event("e1", "Opera", "Cluj", NOW.plusDays(1), 100, 4),
                event("e2", "Ballet", "Cluj", NOW.plusDays(1), 100, 4)));

        assertEquals(List.of("e2", "e1"), ids(office.topEventsByBookedSeats(2)));
    }

    @Test
    void upcomingEventsInCityIgnoresCaseAndPastEvents() {
        TicketOffice office = new TicketOffice(List.of(
                event("e1", "Concert", "Cluj", NOW.plusDays(1), 100, 0),
                event("e2", "Festival", "Cluj", NOW.minusDays(1), 100, 0),
                event("e3", "Play", "Iasi", NOW.plusDays(1), 100, 0)));

        assertEquals(List.of("e1"), ids(office.upcomingEventsIn("cluj", NOW)));
    }

    @Test
    void upcomingEventsAreSortedByStart() {
        TicketOffice office = new TicketOffice(List.of(
                event("e1", "Concert", "Cluj", NOW.plusDays(3), 100, 0),
                event("e2", "Festival", "Cluj", NOW.plusDays(1), 100, 0)));

        assertEquals(List.of("e2", "e1"), ids(office.upcomingEventsIn("Cluj", NOW)));
    }

    @Test
    void bookAllCountsOnlyConfirmedSeats() {
        TicketOffice office = new TicketOffice(List.of(event("e1", 10, 8)));

        int seats = office.bookAll(List.of(
                new BookingRequest("e1", "ana@mail.ro", 2),
                new BookingRequest("e1", "ion@mail.ro", 1),
                new BookingRequest("e9", "dan@mail.ro", 4)));

        assertEquals(2, seats);
    }

    @Test
    void lastBookingIsTheMostRecentOne() {
        TicketOffice office = new TicketOffice(List.of(event("e1", 100, 0)));
        assertEquals(Optional.empty(), office.lastBooking());

        office.book(new BookingRequest("e1", "ana@mail.ro", 2));
        office.book(new BookingRequest("e1", "ion@mail.ro", 3));

        assertEquals("ion@mail.ro", office.lastBooking().orElseThrow().getCustomerEmail());
    }

    @Test
    void bookingsComeInBatches() {
        TicketOffice office = new TicketOffice(List.of(event("e1", 100, 0)));
        for (String email : List.of("a@mail.ro", "b@mail.ro", "c@mail.ro")) {
            office.book(new BookingRequest("e1", email, 1));
        }

        List<List<Booking>> batches = office.bookingsInBatches(2);

        assertEquals(List.of(2, 1), batches.stream().map(List::size).toList());
    }

    private static Event event(String id, int capacity, int bookedSeats) {
        return event(id, "Event " + id, "Cluj", NOW.plusDays(7), capacity, bookedSeats);
    }

    private static Event event(String id, String name, String city, LocalDateTime startsAt, int capacity, int bookedSeats) {
        return Event.builder()
                .id(id)
                .capacity(capacity)
                .bookedSeats(bookedSeats)
                .startsAt(startsAt)
                .name(name)
                .venue(new Venue("Arena", city))
                .build();
    }

    private static List<String> ids(List<Event> events) {
        return events.stream().map(Event::getId).toList();
    }
}
