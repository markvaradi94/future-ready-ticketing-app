package ro.frit.ticketing.event;

import lombok.Getter;
import ro.frit.ticketing.shared.error.ConflictException;

import java.util.Map;

@Getter
public class NotEnoughSeatsException extends ConflictException {
    private final String eventId;
    private final int availableSeats;

    public NotEnoughSeatsException(String eventId, int availableSeats) {
        super("Event " + eventId + " is sold out", Map.of("availableSeats", availableSeats));
        this.eventId = eventId;
        this.availableSeats = availableSeats;
    }
}
