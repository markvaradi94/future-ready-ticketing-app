package ro.frit.ticketing.domain;

import lombok.Builder;

@Builder(toBuilder = true)
public record BookingRequest(String eventId, String customerEmail, int seats) {

    public BookingRequest {
        if (seats <= 0) {
            throw new IllegalArgumentException("Seats must be greater than 0");
        }

        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("Customer email cannot be blank");
        }

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event id cannot be blank");
        }
    }
}
