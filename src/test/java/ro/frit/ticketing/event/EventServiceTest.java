package ro.frit.ticketing.event;

import org.junit.jupiter.api.Test;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test (no Spring): the event rules, with the in-memory repository. The dates are relative to now, at least a
 * day away, so the tests do not depend on when they run.
 */
class EventServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    private final EventRepository eventRepository = new EventRepository();
    private final EventService eventService = new EventService(new EventMapper(), eventRepository);

    @Test
    void findsAnEventById() {
        eventRepository.save(event("e1", 100, 30));

        EventResponse event = eventService.findById("e1");

        assertEquals("e1", event.id());
        assertEquals(70, event.availableSeats());
    }

    @Test
    void unknownEventIsNotFound() {
        EventNotFoundException exception = assertThrows(EventNotFoundException.class, () -> eventService.findById("e9"));

        assertEquals("No event with id e9", exception.getMessage());
    }

    @Test
    void createdEventGetsAnIdAndCanBeFound() {
        EventResponse created = eventService.create(
                new CreateEventRequest("Rock Night", "BT Arena", "Cluj", NOW.plusDays(30), 500));

        assertNotNull(created.id());
        assertEquals("Rock Night", eventService.findById(created.id()).name());
        assertEquals(500, created.availableSeats());
    }

    @Test
    void freeSeatsSkipsFullEvents() {
        eventRepository.save(event("e1", 10, 5));
        eventRepository.save(event("e2", 10, 10));

        assertEquals(List.of("e1"), ids(eventService.findWithFreeSeats()));
    }

    @Test
    void topEventsAreSortedByBookedSeats() {
        eventRepository.save(event("e1", "Concert", "Cluj", NOW.plusDays(1), 100, 5));
        eventRepository.save(event("e2", "Festival", "Cluj", NOW.plusDays(1), 100, 8));
        eventRepository.save(event("e3", "Play", "Cluj", NOW.plusDays(1), 100, 0));

        assertEquals(List.of("e2", "e1"), ids(eventService.findTopByBookedSeats(2)));
    }

    @Test
    void topEventsBreaksTiesByName() {
        eventRepository.save(event("e1", "Opera", "Cluj", NOW.plusDays(1), 100, 4));
        eventRepository.save(event("e2", "Ballet", "Cluj", NOW.plusDays(1), 100, 4));

        assertEquals(List.of("e2", "e1"), ids(eventService.findTopByBookedSeats(2)));
    }

    @Test
    void upcomingEventsInCityIgnoresCaseAndPastEvents() {
        eventRepository.save(event("e1", "Concert", "Cluj", NOW.plusDays(1), 100, 0));
        eventRepository.save(event("e2", "Festival", "Cluj", NOW.minusDays(1), 100, 0));
        eventRepository.save(event("e3", "Play", "Iasi", NOW.plusDays(1), 100, 0));

        assertEquals(List.of("e1"), ids(eventService.findUpcomingIn("cluj")));
    }

    @Test
    void upcomingEventsAreSortedByStart() {
        eventRepository.save(event("e1", "Concert", "Cluj", NOW.plusDays(3), 100, 0));
        eventRepository.save(event("e2", "Festival", "Cluj", NOW.plusDays(1), 100, 0));

        assertEquals(List.of("e2", "e1"), ids(eventService.findUpcomingIn("Cluj")));
    }

    @Test
    void reservingSeatsIncreasesBookedSeats() {
        eventRepository.save(event("e1", 100, 10));

        eventService.reserveSeats("e1", 2);

        assertEquals(12, bookedSeats("e1"));
    }

    @Test
    void secondReservationAddsToBookedSeats() {
        eventRepository.save(event("e1", 100, 0));

        eventService.reserveSeats("e1", 2);
        eventService.reserveSeats("e1", 3);

        assertEquals(5, bookedSeats("e1"));
    }

    @Test
    void exactlyTheFreeSeatsCanBeReserved() {
        eventRepository.save(event("e1", 10, 8));

        eventService.reserveSeats("e1", 2);

        assertEquals(0, eventService.findById("e1").availableSeats());
    }

    @Test
    void tooManySeatsIsRejectedWithTheAvailableSeats() {
        eventRepository.save(event("e1", 10, 8));

        NotEnoughSeatsException exception = assertThrows(NotEnoughSeatsException.class,
                () -> eventService.reserveSeats("e1", 3));

        assertEquals(2, exception.getAvailableSeats());
        assertEquals(8, bookedSeats("e1"));
    }

    @Test
    void startedEventIsRejected() {
        LocalDateTime startsAt = NOW.minusDays(1);
        eventRepository.save(event("e1", "Concert", "Cluj", startsAt, 100, 0));

        EventAlreadyStartedException exception = assertThrows(EventAlreadyStartedException.class,
                () -> eventService.reserveSeats("e1", 2));

        assertEquals(startsAt, exception.getStartsAt());
        assertEquals(0, bookedSeats("e1"));
    }

    @Test
    void startedEventIsCheckedBeforeSeats() {
        eventRepository.save(event("e1", "Concert", "Cluj", NOW.minusDays(1), 10, 10));

        assertThrows(EventAlreadyStartedException.class, () -> eventService.reserveSeats("e1", 2));
    }

    @Test
    void reservingOnAnUnknownEventIsNotFound() {
        assertThrows(EventNotFoundException.class, () -> eventService.reserveSeats("e9", 2));
    }

    private int bookedSeats(String id) {
        return eventRepository.findById(id).orElseThrow().getBookedSeats();
    }

    private static Event event(String id, int capacity, int bookedSeats) {
        return event(id, "Event " + id, "Cluj", NOW.plusDays(7), capacity, bookedSeats);
    }

    private static Event event(String id, String name, String city, LocalDateTime startsAt, int capacity, int bookedSeats) {
        return Event.builder()
                .id(id)
                .name(name)
                .venue(new Venue("Arena", city))
                .startsAt(startsAt)
                .capacity(capacity)
                .bookedSeats(bookedSeats)
                .build();
    }

    private static List<String> ids(List<EventResponse> events) {
        return events.stream().map(EventResponse::id).toList();
    }
}
