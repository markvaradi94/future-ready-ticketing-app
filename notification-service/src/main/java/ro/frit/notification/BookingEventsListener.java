package ro.frit.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static org.springframework.amqp.support.AmqpHeaders.MESSAGE_ID;
import static ro.frit.notification.config.MessagingConfig.BOOKED_EVENT_CHANGED_QUEUE;
import static ro.frit.notification.config.MessagingConfig.BOOKING_CANCELLED_QUEUE;
import static ro.frit.notification.config.MessagingConfig.BOOKING_CONFIRMED_QUEUE;

@Component
@RequiredArgsConstructor
public class BookingEventsListener {
    private final NotificationService notificationService;

    @RabbitListener(queues = BOOKING_CONFIRMED_QUEUE)
    public void onBookingConfirmed(BookingConfirmed event, @Header(MESSAGE_ID) String messageId) {
        if (!event.isValid()) {
            throw new AmqpRejectAndDontRequeueException("Invalid booking confirmed event: %s".formatted(event.bookingId()));
        }

        notificationService.store(messageId, event);
    }

    @RabbitListener(queues = BOOKING_CANCELLED_QUEUE)
    public void onBookingCancelled(BookingCancelled event, @Header(MESSAGE_ID) String messageId) {
        if (!event.isValid()) {
            throw new AmqpRejectAndDontRequeueException("Invalid booking cancelled event: %s".formatted(event.bookingId()));
        }

        notificationService.store(messageId, event);
    }

    // Added after the session (stretch 3).
    @RabbitListener(queues = BOOKED_EVENT_CHANGED_QUEUE)
    public void onBookedEventChanged(BookedEventChanged event, @Header(MESSAGE_ID) String messageId) {
        if (!event.isValid()) {
            throw new AmqpRejectAndDontRequeueException("Invalid booked event changed event: %s".formatted(event.bookingId()));
        }

        notificationService.store(messageId, event);
    }
}
