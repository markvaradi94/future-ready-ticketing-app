package ro.frit.ticketing.booking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingBookingEventPublisher implements BookingEventPublisher {

    @Override
    public void publish(BookingConfirmed bookingConfirmed) {
        log.info("Booking confirmed: {}", bookingConfirmed);
    }

    @Override
    public void publish(BookingCancelled bookingCancelled) {
        log.info("Booking cancelled: {}", bookingCancelled);
    }

    @Override
    public void publish(BookedEventChanged bookedEventChanged) {
        log.info("Booked event changed: {}", bookedEventChanged);
    }
}
