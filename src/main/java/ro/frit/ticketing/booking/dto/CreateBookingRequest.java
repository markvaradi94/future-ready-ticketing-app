package ro.frit.ticketing.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

// The event id is not in the body: it comes from the path, /events/{eventId}/bookings.
public record CreateBookingRequest(@NotBlank @Email String customerEmail,
                                   @Positive int seats) {
}
