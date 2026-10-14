package ro.frit.ticketing.web;

import lombok.Builder;
import ro.frit.ticketing.domain.Event;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record EventResponse(String id, String name, String venueName, String city, LocalDateTime startsAt,
                            int capacity, int availableSeats) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getVenue().name(),
                event.getVenue().city(),
                event.getStartsAt(),
                event.getCapacity(),
                event.availableSeats());
    }
}