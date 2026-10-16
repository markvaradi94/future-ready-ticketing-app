package ro.frit.ticketing.booking;

import ro.frit.ticketing.shared.error.NotFoundException;

public class BookingNotFoundException extends NotFoundException {

    public BookingNotFoundException(String eventId, String bookingId) {
        super("No booking with id " + bookingId + " for event " + eventId);
    }
}
