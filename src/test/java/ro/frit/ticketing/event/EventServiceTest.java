package ro.frit.ticketing.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;
import ro.frit.ticketing.event.dto.EventSearchRequest;
import ro.frit.ticketing.event.dto.UpdateEventRequest;

import java.time.LocalDateTime;
import java.util.List;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.*;

@MongoPersistenceTest
@Import({EventMapper.class, EventService.class})
@RecordApplicationEvents
class EventServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventService eventService;

    @Autowired
    private ApplicationEvents applicationEvents;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }

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
    void searchCombinesTheFiltersThatWereSent() {
        eventRepository.save(event("e1", "Rock Night", "Cluj", NOW.plusDays(10), 100, 0));
        eventRepository.save(event("e2", "Jazz Evening", "Cluj", NOW.plusDays(20), 100, 0));
        eventRepository.save(event("e3", "Rock Opera", "Iasi", NOW.plusDays(30), 100, 0));

        assertEquals(List.of("e1"), ids(eventService.search(new EventSearchRequest("CLUJ", "rock", null, null))));
    }

    @Test
    void searchMatchesTheWholeCityAndTakesTheTextLiterally() {
        eventRepository.save(event("e1", "Rock Night", "Cluj", NOW.plusDays(10), 100, 0));
        eventRepository.save(event("e2", "Rock Night", "Cluj-Napoca", NOW.plusDays(20), 100, 0));

        assertEquals(List.of("e1"), ids(eventService.search(new EventSearchRequest("cluj", null, null, null))));
        assertEquals(List.of(), ids(eventService.search(new EventSearchRequest(".", null, null, null))));
    }

    @Test
    void searchDatesIncludeFromAndExcludeTo() {
        eventRepository.save(event("e1", "Rock Night", "Cluj", NOW.plusDays(10), 100, 0));
        eventRepository.save(event("e2", "Jazz Evening", "Cluj", NOW.plusDays(20), 100, 0));

        assertEquals(List.of("e1"), ids(eventService.search(
                new EventSearchRequest(null, null, NOW.plusDays(10), NOW.plusDays(20)))));
    }

    @Test
    void searchWithoutFiltersReturnsEveryEventByStart() {
        eventRepository.save(event("e2", "Jazz Evening", "Cluj", NOW.plusDays(20), 100, 0));
        eventRepository.save(event("e1", "Rock Night", "Cluj", NOW.plusDays(10), 100, 0));

        assertEquals(List.of("e1", "e2"), ids(eventService.search(new EventSearchRequest(null, null, null, null))));
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
        LocalDateTime startsAt = NOW.minusDays(1).truncatedTo(MILLIS);
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

    @Test
    void updateChangesTheFieldsAndKeepsTheBookedSeats() {
        eventRepository.save(event("e1", 100, 30));

        EventResponse updated = eventService.update("e1",
                new UpdateEventRequest("Renamed", "Hall", "Iasi", NOW.plusDays(9).truncatedTo(MILLIS), 200));

        assertEquals("Renamed", updated.name());
        assertEquals("Iasi", updated.city());
        assertEquals(170, updated.availableSeats());
        assertEquals(30, eventRepository.findById("e1").orElseThrow().getBookedSeats());
    }

    @Test
    void capacityBelowTheSoldSeatsIsRefused() {
        eventRepository.save(event("e1", 100, 30));

        CapacityBelowSoldSeatsException exception = assertThrows(CapacityBelowSoldSeatsException.class,
                () -> eventService.update("e1", new UpdateEventRequest("Event e1", "Arena", "Cluj", NOW.plusDays(7), 20)));

        assertEquals(30, exception.getBookedSeats());
        assertEquals(100, eventRepository.findById("e1").orElseThrow().getCapacity());
    }

    @Test
    void unknownEventCannotBeUpdated() {
        assertThrows(EventNotFoundException.class,
                () -> eventService.update("e9", new UpdateEventRequest("X", "Arena", "Cluj", NOW.plusDays(7), 10)));
    }

    @Test
    void deletedEventIsGoneAndItsCancellationAnnounced() {
        eventRepository.save(event("e1", 100, 2));

        eventService.delete("e1");

        assertFalse(eventRepository.existsById("e1"));
        assertEquals(List.of("e1"), applicationEvents.stream(EventCancelled.class).map(EventCancelled::eventId).toList());
    }

    @Test
    void unknownEventCannotBeDeletedAndNothingIsAnnounced() {
        assertThrows(EventNotFoundException.class, () -> eventService.delete("e9"));

        assertEquals(0, applicationEvents.stream(EventCancelled.class).count());
    }

    @Test
    void releasedSeatsGoBackToTheEvent() {
        eventRepository.save(event("e1", 100, 30));

        eventService.releaseSeats("e1", 2);

        assertEquals(28, eventRepository.findById("e1").orElseThrow().getBookedSeats());
    }

    @Test
    void aNewDateIsAnnounced() {
        eventRepository.save(event("e1", 100, 2));

        eventService.update("e1", new UpdateEventRequest("Event e1", "Arena", "Cluj", NOW.plusDays(9), 100));

        assertEquals(List.of("e1"), applicationEvents.stream(EventChanged.class).map(EventChanged::eventId).toList());
    }

    @Test
    void aCapacityChangeAloneIsNotAnnounced() {
        eventRepository.save(event("e1", 100, 2));
        LocalDateTime sameStart = eventRepository.findById("e1").orElseThrow().getStartsAt();

        eventService.update("e1", new UpdateEventRequest("Event e1", "Arena", "Cluj", sameStart, 200));

        assertEquals(0, applicationEvents.stream(EventChanged.class).count());
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
