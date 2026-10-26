package ro.frit.ticketing.event;

import org.springframework.stereotype.Component;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;
import ro.frit.ticketing.event.dto.UpdateEventRequest;

/**
 * Translates between the {@link Event} entity and the event DTOs, so neither side knows the other's shape.
 */
@Component
public class EventMapper {

    public Event toEntity(CreateEventRequest request) {
        return Event.builder()
                .name(request.name())
                .venue(new Venue(request.venueName(), request.city()))
                .startsAt(request.startsAt())
                .capacity(request.capacity())
                .bookedSeats(0)
                .build();
    }

    public Event update(Event event, UpdateEventRequest request) {
        return event.toBuilder()
                .name(request.name())
                .venue(Venue.builder()
                        .city(request.city())
                        .name(request.venueName())
                        .build())
                .startsAt(request.startsAt())
                .capacity(request.capacity())
                .build();
    }

    public EventResponse toResponse(Event event) {
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
