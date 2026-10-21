package ro.frit.ticketing.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.Event;
import ro.frit.ticketing.event.EventMapper;
import ro.frit.ticketing.event.EventRepository;
import ro.frit.ticketing.event.EventService;
import ro.frit.ticketing.event.Venue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persistence test ({@link MongoPersistenceTest}): 20 one-seat bookings released at the same moment on a
 * capacity-10 event never overbook it. Remove {@code @Version} from {@link Event} and this test fails: that is the
 * proof it tests something.
 */
@MongoPersistenceTest
@Import({BookingService.class, BookingMapper.class, EventService.class, EventMapper.class})
class ConcurrentBookingTest {

    private static final int CAPACITY = 10;
    private static final int THREADS = 20;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void startWithOneEvent() {
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        eventRepository.save(Event.builder()
                .id("e1")
                .name("Rock Night")
                .venue(new Venue("Arena", "Cluj"))
                .startsAt(LocalDateTime.now().plusDays(7))
                .capacity(CAPACITY)
                .build());
    }

    @Test
    void concurrentBookingsNeverExceedTheCapacity() throws Exception {
        // Every thread waits at the start gate, so all 20 bookings are released at the same moment.
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<BookingResponse>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < THREADS; i++) {
                CreateBookingRequest request = new CreateBookingRequest("fan" + i + "@mail.ro", 1);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return bookingService.book("e1", request);
                }));
            }
            try {
                assertTrue(ready.await(10, TimeUnit.SECONDS), "all threads at the start gate");
            } finally {
                // Always open the gate, so a failed wait ends the test instead of hanging pool.close().
                start.countDown();
            }
        }

        int confirmed = 0;
        for (Future<BookingResponse> future : futures) {
            try {
                future.get();
                confirmed++;
            } catch (ExecutionException lostOrSoldOut) {
                // A booking that lost the race, or came after the event filled up: not confirmed.
            }
        }
        int bookedSeats = eventRepository.findById("e1").orElseThrow().getBookedSeats();
        int seatsInBookings = bookingRepository.findAll().stream().mapToInt(Booking::getSeats).sum();

        // No assertion that the event fills up: a booking that loses the race gets a 409.
        assertTrue(seatsInBookings <= CAPACITY, "booked " + seatsInBookings + " seats of " + CAPACITY);
        assertEquals(seatsInBookings, bookedSeats);
        assertEquals(confirmed, seatsInBookings);
        assertTrue(confirmed >= 1, "at least one booking is confirmed");
    }
}
