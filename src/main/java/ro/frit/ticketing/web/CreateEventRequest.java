package ro.frit.ticketing.web;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ro.frit.ticketing.domain.Event;
import ro.frit.ticketing.domain.Venue;

import java.time.LocalDateTime;

// Added after the session (stretch 4).
public record CreateEventRequest(@NotBlank String name, @NotBlank String venueName, @NotBlank String city,
                                 @NotNull @Future LocalDateTime startsAt, @Positive int capacity) {

    public Event toDomain(String id) {
        return Event.builder()
                .id(id)
                .name(name)
                .venue(new Venue(venueName, city))
                .startsAt(startsAt)
                .capacity(capacity)
                .bookedSeats(0)
                .build();
    }
}
