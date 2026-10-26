package ro.frit.ticketing.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateBookingRequest(@NotBlank @Email String customerEmail,
                                   @Positive int seats) {
}