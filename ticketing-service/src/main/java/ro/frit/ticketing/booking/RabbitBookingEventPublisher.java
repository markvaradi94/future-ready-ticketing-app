package ro.frit.ticketing.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static ro.frit.ticketing.config.MessagingConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitBookingEventPublisher implements BookingEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(BookingConfirmed event) {
        rabbitTemplate.convertAndSend(EXCHANGE, BOOKING_CONFIRMED, event);
        log.info("Published {}", event);
    }

    @Override
    public void publish(BookingCancelled event) {
        rabbitTemplate.convertAndSend(EXCHANGE, BOOKING_CANCELLED, event);
        log.info("Published {}", event);
    }

    @Override
    public void publish(BookedEventChanged event) {
        rabbitTemplate.convertAndSend(EXCHANGE, BOOKED_EVENT_CHANGED, event);
        log.info("Published {}", event);
    }
}
