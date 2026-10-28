package ro.frit.notification;

import lombok.Builder;

@Builder(toBuilder = true)
public record BookingCancelled(String bookingId,
                               String eventId,
                               String customerEmail,
                               int seats,
                               Reason reason) {

    public enum Reason {BY_CUSTOMER, EVENT_CANCELLED}

    public boolean isValid() {
        return hasText(bookingId) && hasText(eventId) && hasText(customerEmail) && seats > 0 && reason != null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
