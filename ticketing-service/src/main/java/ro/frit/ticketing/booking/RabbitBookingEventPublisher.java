package ro.frit.ticketing.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static ro.frit.ticketing.config.MessagingConfig.*;

@Component
@RequiredArgsConstructor
public class RabbitBookingEventPublisher implements BookingEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(BookingConfirmed event) {
        rabbitTemplate.convertAndSend(EXCHANGE, BOOKING_CONFIRMED, event);
    }

    @Override
    public void publish(BookingCancelled event) {
        rabbitTemplate.convertAndSend(EXCHANGE, BOOKING_CANCELLED, event);
    }

    @Override
    public void publish(BookedEventChanged event) {
        rabbitTemplate.convertAndSend(EXCHANGE, BOOKED_EVENT_CHANGED, event);
    }
}