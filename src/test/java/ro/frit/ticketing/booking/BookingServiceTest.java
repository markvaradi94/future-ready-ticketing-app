package ro.frit.ticketing.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.BookingSummaryResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.*;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MongoPersistenceTest
@Import({BookingMapper.class, BookingService.class, EventService.class, EventMapper.class})
class BookingServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void confirmedBookingIsStored() {
        eventRepository.save(event("e1", 100, 0));

        BookingResponse booking = bookingService.book("e1", new CreateBookingRequest("ana@mail.ro", 2));

        assertEquals("e1", booking.eventId());
        assertEquals("ana@mail.ro", booking.customerEmail());
        assertEquals(2, booking.seats());
        assertNotNull(booking.bookedAt());
        assertEquals(booking, bookingService.findById("e1", booking.id()));
    }

    @Test
    void confirmedBookingTakesTheSeatsFromTheEvent() {
        eventRepository.save(event("e1", 100, 10));

        bookingService.book("e1", new CreateBookingRequest("ana@mail.ro", 2));

        assertEquals(12, eventRepository.findById("e1").orElseThrow().getBookedSeats());
    }

    @Test
    void unknownEventStoresNothing() {
        assertThrows(EventNotFoundException.class,
                () -> bookingService.book("e9", new CreateBookingRequest("ana@mail.ro", 2)));

        assertTrue(bookingRepository.findAll().isEmpty());
    }

    @Test
    void soldOutStoresNothing() {
        eventRepository.save(event("e1", 10, 8));

        assertThrows(NotEnoughSeatsException.class,
                () -> bookingService.book("e1", new CreateBookingRequest("ana@mail.ro", 3)));

        assertTrue(bookingRepository.findAll().isEmpty());
    }

    @Test
    void unknownBookingIsNotFound() {
        BookingNotFoundException exception = assertThrows(BookingNotFoundException.class,
                () -> bookingService.findById("e1", "b9"));

        assertEquals("No booking with id b9 for event e1", exception.getMessage());
    }

    @Test
    void bookingOfAnotherEventIsNotFound() {
        eventRepository.save(event("e1", 100, 0));
        BookingResponse booking = bookingService.book("e1", new CreateBookingRequest("ana@mail.ro", 2));

        assertThrows(BookingNotFoundException.class, () -> bookingService.findById("e2", booking.id()));
    }

    @Test
    void bookedSeatsPerEventSumsTheBookings() {
        eventRepository.save(event("e1", 100, 0));
        eventRepository.save(event("e2", 100, 0));
        eventRepository.save(event("e3", 100, 0));
        bookingService.book("e1", new CreateBookingRequest("ana@mail.ro", 2));
        bookingService.book("e1", new CreateBookingRequest("ion@mail.ro", 3));
        bookingService.book("e2", new CreateBookingRequest("ana@mail.ro", 1));

        assertEquals(Map.of("e1", 5, "e2", 1), bookingService.bookedSeatsPerEvent());
    }

    @Test
    void summaryCountsAndAveragesTheEventsBookings() {
        eventRepository.save(event("e1", 100, 0));
        eventRepository.save(event("e2", 100, 0));
        bookingService.book("e1", new CreateBookingRequest("ana@mail.ro", 2));
        bookingService.book("e1", new CreateBookingRequest("ion@mail.ro", 3));
        bookingService.book("e1", new CreateBookingRequest("dan@mail.ro", 1));
        bookingService.book("e2", new CreateBookingRequest("ana@mail.ro", 4));

        BookingSummaryResponse summary = bookingService.summaryFor("e1");

        assertEquals(new BookingSummaryResponse("e1", 3, 6, 2.0), summary);
    }

    @Test
    void eventWithoutBookingsHasZeroBookingsAndNoAverage() {
        eventRepository.save(event("e1", 100, 0));

        assertEquals(new BookingSummaryResponse("e1", 0, 0, null), bookingService.summaryFor("e1"));
    }

    @Test
    void unknownEventHasNoSummary() {
        assertThrows(EventNotFoundException.class, () -> bookingService.summaryFor("e9"));
    }

    private static Event event(String id, int capacity, int bookedSeats) {
        return Event.builder()
                .id(id)
                .name("Event " + id)
                .venue(new Venue("Arena", "Cluj"))
                .startsAt(NOW.plusDays(7))
                .capacity(capacity)
                .bookedSeats(bookedSeats)
                .build();
    }
}
