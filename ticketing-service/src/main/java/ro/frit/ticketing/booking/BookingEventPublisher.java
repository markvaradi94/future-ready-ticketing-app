package ro.frit.ticketing.booking;

// the port
public interface BookingEventPublisher {

    void publish(BookingConfirmed bookingConfirmed);

    void publish(BookingCancelled bookingCancelled);

    void publish(BookedEventChanged bookedEventChanged);
}
