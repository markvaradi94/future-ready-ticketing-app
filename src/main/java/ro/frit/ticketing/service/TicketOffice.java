package ro.frit.ticketing.service;

import ro.frit.ticketing.domain.Booking;
import ro.frit.ticketing.domain.BookingRequest;
import ro.frit.ticketing.domain.BookingResult;
import ro.frit.ticketing.domain.Event;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;

//@Service
public class TicketOffice {
    private final Map<String, Event> events = new LinkedHashMap<>();
    private final List<Booking> bookings = new ArrayList<>();

    public TicketOffice(List<Event> events) {
        events.forEach(event -> {
            if (this.events.putIfAbsent(event.getId(), event) != null) {
                throw new IllegalArgumentException("Duplicate event id " + event.getId());
            }
        });
    }

    public List<Event> allEvents() {
        return events.values().stream().toList();
    }

    public BookingResult book(BookingRequest request) {
        Event event = events.get(request.eventId());

        if (event == null) {
            return new BookingResult.UnknownEvent(request.eventId());
        }

        if (!event.getStartsAt().isAfter(LocalDateTime.now())) {
            return new BookingResult.AlreadyStarted(event.getId(), event.getStartsAt());
        }

        if (request.seats() > event.availableSeats()) {
            return new BookingResult.SoldOut(request.eventId(), event.availableSeats());
        }

        Event updated = event.toBuilder()
                .bookedSeats(event.getBookedSeats() + request.seats())
                .build();
        events.put(updated.getId(), updated);

        Booking booking = Booking.builder()
                .id(UUID.randomUUID().toString())
                .eventId(event.getId())
                .customerEmail(request.customerEmail())
                .seats(request.seats())
                .bookedAt(LocalDateTime.now())
                .build();

        bookings.add(booking);

        return new BookingResult.Confirmed(booking);
    }

    public Optional<Event> findEvent(String id) {
        return Optional.ofNullable(events.get(id));
    }

    public List<Event> eventsWithFreeSeats() {
        return events.values().stream()
                .filter(event -> event.availableSeats() > 0)
                .toList();
    }

    public Map<String, Integer> bookedSeatsPerEvent() {
        return bookings.stream()
                .collect(Collectors.groupingBy(Booking::getEventId, Collectors.summingInt(Booking::getSeats)));
    }

    public List<Event> topEventsByBookedSeats(int n) {
        return events.values().stream()
                .sorted(Comparator.comparingInt(Event::getBookedSeats).reversed()
                        .thenComparing(Event::getName))
                .limit(n)
                .toList();
    }

    public List<Event> upcomingEventsIn(String city, LocalDateTime now) {
        return events.values().stream()
                .filter(event -> event.getVenue().city().equalsIgnoreCase(city))
                .filter(event -> event.getStartsAt().isAfter(now))
                .sorted(Comparator.comparing(Event::getStartsAt))
                .toList();
    }

    // Java 21 and 22: a switch over the sealed result; the cases without seats share one branch, and '_' drops their data.
    public int bookAll(List<BookingRequest> requests) {
        return requests.stream()
                .map(this::book)
                .mapToInt(result -> switch (result) {
                    case BookingResult.Confirmed(Booking booking) -> booking.getSeats();
                    case BookingResult.SoldOut _, BookingResult.UnknownEvent _, BookingResult.AlreadyStarted _ -> 0;
                })
                .sum();
    }

    // Java 21: getLast() on a list.
    public Optional<Booking> lastBooking() {
        return bookings.isEmpty() ? Optional.empty() : Optional.of(bookings.getLast());
    }

    // Java 24: a gatherer cuts the stream into groups of the given size.
    public List<List<Booking>> bookingsInBatches(int size) {
        return bookings.stream()
                .gather(Gatherers.windowFixed(size))
                .toList();
    }
}
