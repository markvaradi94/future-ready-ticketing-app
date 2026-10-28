package ro.frit.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ro.frit.notification.config.TestcontainersConfiguration;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ro.frit.notification.config.MessagingConfig.BOOKED_EVENT_CHANGED;
import static ro.frit.notification.config.MessagingConfig.BOOKED_EVENT_CHANGED_DEAD_LETTER_QUEUE;
import static ro.frit.notification.config.MessagingConfig.BOOKING_CANCELLED;
import static ro.frit.notification.config.MessagingConfig.BOOKING_CANCELLED_DEAD_LETTER_QUEUE;
import static ro.frit.notification.config.MessagingConfig.BOOKING_CONFIRMED;
import static ro.frit.notification.config.MessagingConfig.BOOKING_CONFIRMED_DEAD_LETTER_QUEUE;
import static ro.frit.notification.config.MessagingConfig.EXCHANGE;

/**
 * Integration test (the whole service, on MongoDB and RabbitMQ containers): messages are published to ticketing's
 * exchange as raw JSON, the way ticketing sends them, type header included, and the listeners consume them.
 * Added after the session.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingEventsListenerTest {
    private static final Duration WAIT = Duration.ofSeconds(10);
    private static final String CONFIRMED_TYPE = "ro.frit.ticketing.booking.BookingConfirmed";
    private static final String CANCELLED_TYPE = "ro.frit.ticketing.booking.BookingCancelled";
    private static final String CHANGED_TYPE = "ro.frit.ticketing.booking.BookedEventChanged";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void startEmpty() {
        amqpAdmin.purgeQueue(BOOKING_CONFIRMED_DEAD_LETTER_QUEUE, false);
        amqpAdmin.purgeQueue(BOOKING_CANCELLED_DEAD_LETTER_QUEUE, false);
        amqpAdmin.purgeQueue(BOOKED_EVENT_CHANGED_DEAD_LETTER_QUEUE, false);
        notificationRepository.deleteAll();
    }

    @Test
    void confirmedBookingStoresOneNotificationWithReadableText() {
        publish(BOOKING_CONFIRMED, CONFIRMED_TYPE, "m-1", bookingConfirmed("b-1", 2));

        await().atMost(WAIT).until(() -> notificationRepository.findByMessageId("m-1").isPresent());
        Notification notification = notificationRepository.findByMessageId("m-1").orElseThrow();
        assertEquals("b-1", notification.getBookingId());
        assertEquals("ana@mail.ro", notification.getCustomerEmail());
        assertEquals("2 seat(s) for Rock Night on 12 Jun 2030 at 20:00 are confirmed.", notification.getText());
    }

    @Test
    void cancelledBookingStoresOneNotificationWithReadableText() {
        publish(BOOKING_CANCELLED, CANCELLED_TYPE, "m-1", bookingCancelled("b-1", 2, "EVENT_CANCELLED"));

        await().atMost(WAIT).until(() -> notificationRepository.findByMessageId("m-1").isPresent());
        Notification notification = notificationRepository.findByMessageId("m-1").orElseThrow();
        assertEquals("b-1", notification.getBookingId());
        assertEquals("2 seat(s) for event rock-cluj are cancelled: the event was called off.", notification.getText());
    }

    @Test
    void changedEventStoresOneNotificationWithReadableText() {
        publish(BOOKED_EVENT_CHANGED, CHANGED_TYPE, "m-1", bookedEventChanged("b-1", 2));

        await().atMost(WAIT).until(() -> notificationRepository.findByMessageId("m-1").isPresent());
        Notification notification = notificationRepository.findByMessageId("m-1").orElseThrow();
        assertEquals("b-1", notification.getBookingId());
        assertEquals("Your event changed: 2 seat(s) for Rock Night, now at Sala Polivalenta, Cluj, on 13 Jun 2030 at 21:00.",
                notification.getText());
    }

    @Test
    void invalidChangeLandsInItsOwnDeadLetterQueue() {
        publish(BOOKED_EVENT_CHANGED, CHANGED_TYPE, "m-1", bookedEventChanged("b-1", 0));

        assertNotNull(rabbitTemplate.receive(BOOKED_EVENT_CHANGED_DEAD_LETTER_QUEUE, WAIT.toMillis()),
                "nothing in the dead-letter queue");
        assertEquals(0, notificationRepository.count());
    }

    @Test
    void sameMessageDeliveredTwiceStoresOneNotification() {
        publish(BOOKING_CONFIRMED, CONFIRMED_TYPE, "m-1", bookingConfirmed("b-1", 2));
        publish(BOOKING_CONFIRMED, CONFIRMED_TYPE, "m-1", bookingConfirmed("b-1", 2));
        // One listener takes the queue in order: once m-2 is stored, both copies of m-1 have been handled.
        publish(BOOKING_CONFIRMED, CONFIRMED_TYPE, "m-2", bookingConfirmed("b-2", 1));

        await().atMost(WAIT).until(() -> notificationRepository.findByMessageId("m-2").isPresent());
        assertEquals(2, notificationRepository.count());
        assertNull(rabbitTemplate.receive(BOOKING_CONFIRMED_DEAD_LETTER_QUEUE, 500), "the duplicate was dead-lettered");
    }

    @Test
    void unreadableMessageLandsInTheDeadLetterQueue() {
        publish(BOOKING_CONFIRMED, CONFIRMED_TYPE, "m-1", "this is not JSON");

        Message deadLetter = rabbitTemplate.receive(BOOKING_CONFIRMED_DEAD_LETTER_QUEUE, WAIT.toMillis());
        assertNotNull(deadLetter, "nothing in the dead-letter queue");
        assertEquals("this is not JSON", new String(deadLetter.getBody(), StandardCharsets.UTF_8));
        assertEquals(0, notificationRepository.count());
    }

    @Test
    void invalidCancellationLandsInItsOwnDeadLetterQueue() {
        publish(BOOKING_CANCELLED, CANCELLED_TYPE, "m-1", bookingCancelled("b-1", 0, "BY_CUSTOMER"));

        assertNotNull(rabbitTemplate.receive(BOOKING_CANCELLED_DEAD_LETTER_QUEUE, WAIT.toMillis()),
                "nothing in the dead-letter queue");
        assertEquals(0, notificationRepository.count());
    }

    private void publish(String routingKey, String type, String messageId, String json) {
        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(messageId)
                .setHeader("__TypeId__", type)
                .build();
        rabbitTemplate.send(EXCHANGE, routingKey, message);
    }

    private static String bookingConfirmed(String bookingId, int seats) {
        return """
                {"bookingId":"%s","eventId":"rock-cluj","eventName":"Rock Night","eventStartsAt":"2030-06-12T20:00:00",
                 "customerEmail":"ana@mail.ro","seats":%d,"bookedAt":"2026-10-28T18:30:00.123"}
                """.formatted(bookingId, seats);
    }

    private static String bookedEventChanged(String bookingId, int seats) {
        return """
                {"bookingId":"%s","eventId":"rock-cluj","customerEmail":"ana@mail.ro","seats":%d,"eventName":"Rock Night",
                 "venueName":"Sala Polivalenta","city":"Cluj","eventStartsAt":"2030-06-13T21:00:00"}
                """.formatted(bookingId, seats);
    }

    private static String bookingCancelled(String bookingId, int seats, String reason) {
        return """
                {"bookingId":"%s","eventId":"rock-cluj","customerEmail":"ana@mail.ro","seats":%d,"reason":"%s"}
                """.formatted(bookingId, seats, reason);
    }
}
