package ro.frit.ticketing.event;

import lombok.Getter;
import ro.frit.ticketing.shared.error.ConflictException;

import java.util.Map;

@Getter
public class CapacityBelowSoldSeatsException extends ConflictException {
    private final String eventId;
    private final int bookedSeats;

    public CapacityBelowSoldSeatsException(String eventId, int bookedSeats) {
        super("Event %s has already sold %s seats.".formatted(eventId, bookedSeats), Map.of("bookedSeats", bookedSeats));
        this.eventId = eventId;
        this.bookedSeats = bookedSeats;
    }
}
