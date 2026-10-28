package ro.frit.ticketing.booking;

import lombok.Builder;
import ro.frit.ticketing.event.dto.EventResponse;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record BookingConfirmed(String bookingId,
                               String eventId,
                               String eventName,
                               LocalDateTime eventStartsAt,
                               String customerEmail,
                               int seats,
                               LocalDateTime bookedAt) {

    static BookingConfirmed of(Booking booking, EventResponse event) {
        return BookingConfirmed.builder()
                .bookingId(booking.getId())
                .eventId(booking.getEventId())
                .eventName(event.name())
                .eventStartsAt(event.startsAt())
                .customerEmail(booking.getCustomerEmail())
                .seats(booking.getSeats())
                .bookedAt(booking.getBookedAt())
                .build();
    }
}