package ro.frit.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static org.springframework.amqp.support.AmqpHeaders.MESSAGE_ID;
import static ro.frit.notification.config.MessagingConfig.BOOKED_EVENT_CHANGED_QUEUE;
import static ro.frit.notification.config.MessagingConfig.BOOKING_CANCELLED_QUEUE;
import static ro.frit.notification.config.MessagingConfig.BOOKING_CONFIRMED_QUEUE;

// The booking request's correlation id arrives in a header, next to the message id. It is optional: a message
// without one is still handled. Everything this message causes is logged with it.
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventsListener {
    private final NotificationService notificationService;

    @RabbitListener(queues = BOOKING_CONFIRMED_QUEUE)
    public void onBookingConfirmed(BookingConfirmed event, @Header(MESSAGE_ID) String messageId,
                                   @Header(name = CorrelationIdFilter.HEADER, required = false) String correlationId) {
        withCorrelationId(correlationId, () -> {
            log.info("Received BookingConfirmed for booking {}", event.bookingId());
            if (!event.isValid()) {
                throw new AmqpRejectAndDontRequeueException("Invalid booking confirmed event: %s".formatted(event.bookingId()));
            }
            notificationService.store(messageId, event);
        });
    }

    @RabbitListener(queues = BOOKING_CANCELLED_QUEUE)
    public void onBookingCancelled(BookingCancelled event, @Header(MESSAGE_ID) String messageId,
                                   @Header(name = CorrelationIdFilter.HEADER, required = false) String correlationId) {
        withCorrelationId(correlationId, () -> {
            log.info("Received BookingCancelled for booking {}", event.bookingId());
            if (!event.isValid()) {
                throw new AmqpRejectAndDontRequeueException("Invalid booking cancelled event: %s".formatted(event.bookingId()));
            }
            notificationService.store(messageId, event);
        });
    }

    // Added after Session 6 (stretch 3).
    @RabbitListener(queues = BOOKED_EVENT_CHANGED_QUEUE)
    public void onBookedEventChanged(BookedEventChanged event, @Header(MESSAGE_ID) String messageId,
                                     @Header(name = CorrelationIdFilter.HEADER, required = false) String correlationId) {
        withCorrelationId(correlationId, () -> {
            log.info("Received BookedEventChanged for booking {}", event.bookingId());
            if (!event.isValid()) {
                throw new AmqpRejectAndDontRequeueException("Invalid booked event changed event: %s".formatted(event.bookingId()));
            }
            notificationService.store(messageId, event);
        });
    }

    private static void withCorrelationId(String correlationId, Runnable handling) {
        if (correlationId != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);
        }
        try {
            handling.run();
        } finally {
            // The listener thread takes the next message too: it must not inherit this id.
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }
}
