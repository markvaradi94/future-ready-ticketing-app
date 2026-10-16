package ro.frit.ticketing.event;

import ro.frit.ticketing.shared.error.NotFoundException;

public class EventNotFoundException extends NotFoundException {

    public EventNotFoundException(String eventId) {
        super("No event with id " + eventId);
    }
}
