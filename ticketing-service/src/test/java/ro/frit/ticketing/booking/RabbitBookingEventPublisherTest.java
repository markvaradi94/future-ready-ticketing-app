package ro.frit.ticketing.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.config.TestcontainersConfiguration;
import ro.frit.ticketing.event.Event;
import ro.frit.ticketing.event.EventRepository;
import ro.frit.ticketing.event.Venue;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static ro.frit.ticketing.config.MessagingConfig.BOOKING_CONFIRMED;
import static ro.frit.ticketing.config.MessagingConfig.EXCHANGE;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class RabbitBookingEventPublisherTest {
    private static final String QUEUE = "test.booking-confirmed";
    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2030, 6, 12, 20, 0);

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        bookingRepository.deleteAll();

        Queue queue = new Queue(QUEUE);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.purgeQueue(QUEUE, false);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue)
                .to(new TopicExchange(EXCHANGE))
                .with(BOOKING_CONFIRMED));
    }

    @Test
    void confirmedBookingPublishesOneJsonMessageWithTheAgreedFields() {
        eventRepository.save(Event.builder()
                .id("rock-cluj")
                .name("Rock Night")
                .venue(new Venue("BT Arena", "Cluj"))
                .startsAt(STARTS_AT)
                .capacity(10)
                .build());

        BookingResponse booking = bookingService.book("rock-cluj", new CreateBookingRequest("ana@mail.ro", 2));

        Message message = rabbitTemplate.receive(QUEUE, 5_000);
        assertNotNull(message, "no message published");
        assertEquals("application/json", message.getMessageProperties().getContentType());
        assertNotNull(message.getMessageProperties().getMessageId(), "no message id");
        JsonNode json = jsonMapper.readTree(message.getBody());
        assertEquals(booking.id(), json.get("bookingId").asString());
        assertEquals("rock-cluj", json.get("eventId").asString());
        assertEquals("Rock Night", json.get("eventName").asString());
        assertEquals(STARTS_AT, LocalDateTime.parse(json.get("eventStartsAt").asString()));
        assertEquals("ana@mail.ro", json.get("customerEmail").asString());
        assertEquals(2, json.get("seats").asInt());
        assertEquals(booking.bookedAt(), LocalDateTime.parse(json.get("bookedAt").asString()));

        assertNull(rabbitTemplate.receive(QUEUE, 500), "more than one message published");
    }
}
