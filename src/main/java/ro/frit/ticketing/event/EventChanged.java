package ro.frit.ticketing.event;

import lombok.Builder;

import java.time.LocalDateTime;

// Added after the session (lab round 3): announced when something a customer cares about changes, the name, the venue
// or the time. The capacity is ours alone, so changing only that announces nothing.
@Builder(toBuilder = true)
public record EventChanged(String eventId, String name, String venueName, String city, LocalDateTime startsAt) {

    static EventChanged of(Event event) {
        return EventChanged.builder()
                .eventId(event.getId())
                .name(event.getName())
                .venueName(event.getVenue().name())
                .city(event.getVenue().city())
                .startsAt(event.getStartsAt())
                .build();
    }
}
