package ro.frit.ticketing.booking;

import lombok.Builder;

@Builder(toBuilder = true)
public record BookingCancelled(String bookingId,
                               String eventId,
                               String customerEmail,
                               int seats,
                               Reason reason) {

    public enum Reason {BY_CUSTOMER, EVENT_CANCELLED}

    static BookingCancelled of(Booking booking, Reason reason) {
        return BookingCancelled.builder()
                .bookingId(booking.getId())
                .eventId(booking.getEventId())
                .customerEmail(booking.getCustomerEmail())
                .seats(booking.getSeats())
                .reason(reason)
                .build();
    }
}
