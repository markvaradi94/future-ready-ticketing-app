package ro.frit.ticketing.booking;

import lombok.Builder;
import ro.frit.ticketing.event.EventChanged;

import java.time.LocalDateTime;

// Added after the session (lab round 3): what a customer must learn when the event they booked changes, their booking
// and the event as it is now.
@Builder(toBuilder = true)
public record BookedEventChanged(String bookingId,
                                 String eventId,
                                 String customerEmail,
                                 int seats,
                                 String eventName,
                                 String venueName,
                                 String city,
                                 LocalDateTime eventStartsAt) {

    static BookedEventChanged of(Booking booking, EventChanged changed) {
        return BookedEventChanged.builder()
                .bookingId(booking.getId())
                .eventId(booking.getEventId())
                .customerEmail(booking.getCustomerEmail())
                .seats(booking.getSeats())
                .eventName(changed.name())
                .venueName(changed.venueName())
                .city(changed.city())
                .eventStartsAt(changed.startsAt())
                .build();
    }
}
