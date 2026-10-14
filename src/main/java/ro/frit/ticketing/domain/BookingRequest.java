package ro.frit.ticketing.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder(toBuilder = true)
public record BookingRequest(@NotBlank String eventId, @NotBlank @Email String customerEmail, @Positive int seats) {
}
