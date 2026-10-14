package ro.frit.ticketing.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import ro.frit.ticketing.domain.BookingRequest;

public record BookingBody(@NotBlank @Email String customerEmail, @Positive int seats) {

    public BookingRequest toRequest(String eventId) {
        return new BookingRequest(eventId, customerEmail, seats);
    }
}
