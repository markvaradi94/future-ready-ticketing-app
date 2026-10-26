package ro.frit.ticketing.demo;

import com.mongodb.ExplainVerbosity;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.booking.Booking;
import ro.frit.ticketing.booking.BookingMapper;
import ro.frit.ticketing.booking.BookingRepository;
import ro.frit.ticketing.booking.BookingService;
import ro.frit.ticketing.event.EventMapper;
import ro.frit.ticketing.event.EventService;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Session 4 demo, steps 1 and 2: 100,000 bookings over 100 events, each question answered the way we did before and
 * the way we do now, and what it cost printed. Nothing is asserted.
 *
 * <pre>
 * ./gradlew test --tests '*MongoWorkDemo.seatsPerEvent' -i      (step 1: the aggregation)
 * ./gradlew test --tests '*MongoWorkDemo.oneEventsBookings' -i  (step 2: the index)
 * </pre>
 *
 * Look for the {@code SEATS} and {@code FIND} lines.
 */
@Tag("demo")
@MongoPersistenceTest
@Import({BookingService.class, BookingMapper.class, EventService.class, EventMapper.class})
class MongoWorkDemo {

    private static final int BOOKINGS = 100_000;
    private static final int EVENTS = 100;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void storeManyBookings() {
        // Written straight into the collection, so filling it takes a second, not a minute.
        var bookings = mongoTemplate.getCollection("bookings");
        bookings.deleteMany(new Document());
        List<Document> documents = IntStream.range(0, BOOKINGS)
                .mapToObj(i -> new Document("eventId", "e" + i % EVENTS)
                        .append("customerEmail", "fan" + i + "@mail.ro")
                        .append("seats", 1 + i % 4)
                        .append("bookedAt", new Date()))
                .toList();
        bookings.insertMany(documents);
    }

    // Step 1: seats per event, summed in Java (before) and by MongoDB (now). Three runs each, because the first one
    // also warms up the JVM.
    @Test
    void seatsPerEvent() {
        measure("in Java, every booking sent to the app", () -> bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(Booking::getEventId, Collectors.summingInt(Booking::getSeats))));
        measure("by MongoDB, the aggregation", bookingService::bookedSeatsPerEvent);
    }

    // Step 2: how MongoDB finds one event's bookings. explain() runs the query and reports what it did. The hint
    // $natural forces a collection scan, which is what happens without the @Indexed on Booking.eventId.
    @Test
    void oneEventsBookings() {
        explain("without the index", mongoTemplate.getCollection("bookings")
                .find(new Document("eventId", "e7")).hint(new Document("$natural", 1)));
        explain("with the index", mongoTemplate.getCollection("bookings")
                .find(new Document("eventId", "e7")));
    }

    private void measure(String how, Supplier<Map<String, Integer>> seatsPerEvent) {
        for (int run = 1; run <= 3; run++) {
            long sentBefore = documentsSentToTheApp();
            long start = System.nanoTime();
            Map<String, Integer> seats = seatsPerEvent.get();
            long millis = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("SEATS %s, run %d: %d events, %,d documents sent to the app, %d ms%n",
                    how, run, seats.size(), documentsSentToTheApp() - sentBefore, millis);
        }
    }

    private static void explain(String how, FindIterable<Document> find) {
        Document explained = find.explain(ExplainVerbosity.EXECUTION_STATS);
        Document stats = explained.get("executionStats", Document.class);
        String plan = explained.get("queryPlanner", Document.class).toJson().contains("\"IXSCAN\"") ? "IXSCAN" : "COLLSCAN";
        System.out.printf("FIND e7 %s: %s, %,d documents examined to return %,d, %d ms%n",
                how, plan, stats.getInteger("totalDocsExamined"), stats.getInteger("nReturned"),
                stats.getInteger("executionTimeMillis"));
    }

    // MongoDB counts every document it returns to a client; the difference before and after a call is that call's.
    private long documentsSentToTheApp() {
        Document status = mongoTemplate.executeCommand(new Document("serverStatus", 1));
        Number returned = status.get("metrics", Document.class).get("document", Document.class).get("returned", Number.class);
        return returned.longValue();
    }
}
