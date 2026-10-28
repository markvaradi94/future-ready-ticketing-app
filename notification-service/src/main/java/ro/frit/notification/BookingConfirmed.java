package ro.frit.notification;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record BookingConfirmed(String bookingId,
                               String eventId,
                               String eventName,
                               LocalDateTime eventStartsAt,
                               String customerEmail,
                               int seats,
                               LocalDateTime bookedAt) {

    public boolean isValid() {
        return hasText(bookingId) && hasText(eventId) && hasText(eventName) && hasText(customerEmail)
                && seats > 0 && eventStartsAt != null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
