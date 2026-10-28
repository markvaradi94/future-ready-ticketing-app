package ro.frit.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class MessagingConfig {
    public static final String EXCHANGE = "ticketing.events";
    public static final String BOOKING_CONFIRMED = "booking.confirmed";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String BOOKED_EVENT_CHANGED = "booked.event-changed";
    public static final String BOOKING_CONFIRMED_QUEUE = "notification.booking-confirmed";
    public static final String BOOKING_CANCELLED_QUEUE = "notification.booking-cancelled";
    public static final String BOOKED_EVENT_CHANGED_QUEUE = "notification.booked-event-changed";
    public static final String DEAD_LETTER_EXCHANGE = "notification.dlx";
    public static final String BOOKING_CONFIRMED_DEAD_LETTER_QUEUE = "notification.booking-confirmed.dlq";
    public static final String BOOKING_CANCELLED_DEAD_LETTER_QUEUE = "notification.booking-cancelled.dlq";
    public static final String BOOKED_EVENT_CHANGED_DEAD_LETTER_QUEUE = "notification.booked-event-changed.dlq";

    // Declared here too, the same as in ticketing: a binding needs its exchange, and either service may start first.
    @Bean
    TopicExchange ticketingExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue bookingConfirmedQueue() {
        return QueueBuilder.durable(BOOKING_CONFIRMED_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(BOOKING_CONFIRMED_DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Binding bookingConfirmedBinding() {
        return BindingBuilder.bind(bookingConfirmedQueue())
                .to(ticketingExchange())
                .with(BOOKING_CONFIRMED);
    }

    @Bean
    Queue bookingCancelledQueue() {
        return QueueBuilder.durable(BOOKING_CANCELLED_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(BOOKING_CANCELLED_DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Binding bookingCancelledBinding() {
        return BindingBuilder.bind(bookingCancelledQueue())
                .to(ticketingExchange())
                .with(BOOKING_CANCELLED);
    }

    // Added after the session (stretch 3): the third fact ticketing publishes, with its own queue and dead-letter queue.
    @Bean
    Queue bookedEventChangedQueue() {
        return QueueBuilder.durable(BOOKED_EVENT_CHANGED_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(BOOKED_EVENT_CHANGED_DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Binding bookedEventChangedBinding() {
        return BindingBuilder.bind(bookedEventChangedQueue())
                .to(ticketingExchange())
                .with(BOOKED_EVENT_CHANGED);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    // One dead-letter queue per queue: each queue's dead letters go to the exchange with their own routing key, and
    // a routing key that nothing is bound to drops the message.
    @Bean
    Queue bookingConfirmedDeadLetterQueue() {
        return QueueBuilder.durable(BOOKING_CONFIRMED_DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Binding bookingConfirmedDeadLetterBinding() {
        return BindingBuilder.bind(bookingConfirmedDeadLetterQueue())
                .to(deadLetterExchange())
                .with(BOOKING_CONFIRMED_DEAD_LETTER_QUEUE);
    }

    @Bean
    Queue bookingCancelledDeadLetterQueue() {
        return QueueBuilder.durable(BOOKING_CANCELLED_DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Binding bookingCancelledDeadLetterBinding() {
        return BindingBuilder.bind(bookingCancelledDeadLetterQueue())
                .to(deadLetterExchange())
                .with(BOOKING_CANCELLED_DEAD_LETTER_QUEUE);
    }

    @Bean
    Queue bookedEventChangedDeadLetterQueue() {
        return QueueBuilder.durable(BOOKED_EVENT_CHANGED_DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Binding bookedEventChangedDeadLetterBinding() {
        return BindingBuilder.bind(bookedEventChangedDeadLetterQueue())
                .to(deadLetterExchange())
                .with(BOOKED_EVENT_CHANGED_DEAD_LETTER_QUEUE);
    }

    @Bean
    MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
