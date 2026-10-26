package ro.frit.ticketing.booking;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record BookingConfirmed(String bookingId,
                               String eventId,
                               String customerEmail,
                               int seats,
                               LocalDateTime bookedAt) {

    static BookingConfirmed of(Booking booking) {
        return BookingConfirmed.builder()
                .bookingId(booking.getId())
                .eventId(booking.getEventId())
                .customerEmail(booking.getCustomerEmail())
                .seats(booking.getSeats())
                .bookedAt(booking.getBookedAt())
                .build();
    }
}