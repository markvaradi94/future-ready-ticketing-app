package ro.frit.ticketing.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder(toBuilder = true)
public record BookingRequest(@NotBlank String eventId, @NotBlank @Email String customerEmail, @Positive int seats) {

    public BookingRequest {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event id must not be blank");
        }
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("Customer email must not be blank");
        }
        if (seats <= 0) {
            throw new IllegalArgumentException("Seats must be positive, got " + seats);
        }
    }
}
