package ro.frit.ticketing.event;

import lombok.Getter;
import ro.frit.ticketing.shared.error.ConflictException;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
public class EventAlreadyStartedException extends ConflictException {
    private final String eventId;
    private final LocalDateTime startsAt;

    public EventAlreadyStartedException(String eventId, LocalDateTime startsAt) {
        super("Event " + eventId + " has already started", Map.of("startsAt", startsAt));
        this.eventId = eventId;
        this.startsAt = startsAt;
    }
}
