package ro.frit.ticketing.domain;

import java.time.LocalDateTime;
import java.util.*;

public class TicketOffice {
    private Map<String, Event> events = new LinkedHashMap<>();
    private final List<Booking> bookings = new ArrayList<>();

    public TicketOffice(List<Event> events) {
        events.forEach(event -> this.events.put(event.getId(), event));
    }

    public BookingResult book(BookingRequest request) {
        Event event = events.get(request.eventId());

        if (event == null) {
            return new BookingResult.UnknownEvent(request.eventId());
        }

        if (request.seats() > event.availableSeats()) {
            return new BookingResult.SoldOut(request.eventId(), event.availableSeats());
        }

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
}