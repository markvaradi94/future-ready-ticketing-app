package ro.frit.ticketing.event;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record EventCancelled(String eventId, String name, LocalDateTime startsAt) {

    static EventCancelled of(Event event) {
        return EventCancelled.builder()
                .eventId(event.getId())
                .name(event.getName())
                .startsAt(event.getStartsAt())
                .build();
    }
}
