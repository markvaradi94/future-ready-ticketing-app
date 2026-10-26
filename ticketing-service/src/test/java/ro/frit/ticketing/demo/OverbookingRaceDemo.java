package ro.frit.ticketing.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.booking.Booking;
import ro.frit.ticketing.booking.BookingMapper;
import ro.frit.ticketing.booking.BookingRepository;
import ro.frit.ticketing.booking.BookingService;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.Event;
import ro.frit.ticketing.event.EventMapper;
import ro.frit.ticketing.event.EventRepository;
import ro.frit.ticketing.event.EventService;
import ro.frit.ticketing.event.Venue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Session 4 demo, step 3: 20 one-seat bookings released at the same moment on a capacity-10 event, ten times over.
 * It prints what happened and asserts nothing; {@code ConcurrentBookingTest} is the real test.
 *
 * <pre>./gradlew test --tests '*OverbookingRaceDemo' -i</pre>
 *
 * Look for the {@code RACE} lines. With {@code @Version} on {@link Event}: never more than 10 seats, and the losers
 * get an {@code OptimisticLockingFailureException}. Remove {@code @Version} and run it again: more seats in bookings
 * than the event has, and the event's own count disagrees with its bookings. Put it back afterwards.
 * <p>
 * The retry on {@code reserveSeats} is not active here: {@code @EnableResilientMethods} is on the application class,
 * and a persistence test does not start the whole application.
 */
@Tag("demo")
@MongoPersistenceTest
@Import({BookingService.class, BookingMapper.class, EventService.class, EventMapper.class})
class OverbookingRaceDemo {

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
                .id("race")
                .name("Race Night")
                .venue(new Venue("Arena", "Cluj"))
                .startsAt(LocalDateTime.now().plusDays(7))
                .capacity(CAPACITY)
                .build());
    }

    @RepeatedTest(10)
    void twentyBookingsAtOnce(RepetitionInfo repetition) throws Exception {
        // Every thread waits at the start gate, so all 20 bookings are released at the same moment.
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> outcomes = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < THREADS; i++) {
                CreateBookingRequest request = new CreateBookingRequest("fan" + i + "@mail.ro", 1);
                outcomes.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        bookingService.book("race", request);
                        return "Confirmed";
                    } catch (RuntimeException e) {
                        return e.getClass().getSimpleName();
                    }
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
        }

        List<String> results = new ArrayList<>();
        for (Future<String> outcome : outcomes) {
            results.add(outcome.get());
        }
        int bookedSeats = eventRepository.findById("race").orElseThrow().getBookedSeats();
        int seatsInBookings = bookingRepository.findAll().stream().mapToInt(Booking::getSeats).sum();

        System.out.printf("RACE run %d: %d seats in bookings, %d on the event, capacity %d, outcomes %s%n",
                repetition.getCurrentRepetition(), seatsInBookings, bookedSeats, CAPACITY,
                results.stream().collect(Collectors.groupingBy(r -> r, TreeMap::new, Collectors.counting())));
    }
}
