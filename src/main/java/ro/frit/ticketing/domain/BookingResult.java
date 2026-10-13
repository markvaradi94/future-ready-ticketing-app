package ro.frit.ticketing.domain;

import java.time.LocalDateTime;

public sealed interface BookingResult permits BookingResult.AlreadyStarted, BookingResult.Confirmed, BookingResult.SoldOut, BookingResult.UnknownEvent {

    record Confirmed(Booking booking) implements BookingResult {}

    record SoldOut(String eventId, int availableSeats) implements BookingResult {
    }

    record UnknownEvent(String eventId) implements BookingResult {
    }

    record AlreadyStarted(String eventId, LocalDateTime startsAt) implements BookingResult {
    }

    default String describe() {
        return switch (this) {
            case SoldOut(String eventId, int availableSeats) -> String.format("Event %s SOLD OUT (%d seats available)", eventId, availableSeats);
            case UnknownEvent(String eventId) -> String.format("Event %s NOT FOUND", eventId);
            case Confirmed(Booking booking) -> String.format("Event %s CONFIRMED", booking.getEventId());
            case AlreadyStarted(String eventId, LocalDateTime startsAt) -> String.format("Event %s HAS ALREADY STARTED", eventId);
        };
    }
}