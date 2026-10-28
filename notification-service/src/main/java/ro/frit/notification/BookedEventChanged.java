package ro.frit.notification;

import lombok.Builder;

import java.time.LocalDateTime;

// Added after the session (stretch 3): the event a customer booked has changed; the booking, and the event as it is now.
@Builder(toBuilder = true)
public record BookedEventChanged(String bookingId,
                                 String eventId,
                                 String customerEmail,
                                 int seats,
                                 String eventName,
                                 String venueName,
                                 String city,
                                 LocalDateTime eventStartsAt) {

    public boolean isValid() {
        return hasText(bookingId) && hasText(eventId) && hasText(customerEmail) && hasText(eventName)
                && hasText(venueName) && hasText(city) && seats > 0 && eventStartsAt != null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
