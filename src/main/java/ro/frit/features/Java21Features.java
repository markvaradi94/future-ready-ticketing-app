package ro.frit.features;

import ro.frit.ticketing.domain.Booking;
import ro.frit.ticketing.domain.BookingResult;
import ro.frit.ticketing.domain.BookingResult.AlreadyStarted;
import ro.frit.ticketing.domain.BookingResult.Confirmed;
import ro.frit.ticketing.domain.BookingResult.SoldOut;
import ro.frit.ticketing.domain.BookingResult.UnknownEvent;
import ro.frit.ticketing.domain.Event;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Java 18 to 21 (2023, LTS): patterns in switch, record patterns, sequenced collections, virtual threads.
 */
public class Java21Features {

    public static void main(String[] args) throws InterruptedException {
        patternsInSwitch();
        sequencedCollections();
        virtualThreads();
    }

    // Java 21: each case is a type, the record is taken apart in the label, and there is no default.
    private static void patternsInSwitch() {
        System.out.println("--- Java 21: patterns in switch, record patterns");
        List<BookingResult> results = List.of(
                new SoldOut("rock-cluj", 20),
                new UnknownEvent("nope"));

        for (BookingResult result : results) {
            String message = switch (result) {
                case Confirmed(Booking booking) -> "confirmed: " + booking.getId();
                case SoldOut(String eventId, int availableSeats) -> eventId + " has only " + availableSeats + " seats left";
                case UnknownEvent(String eventId) -> "no event " + eventId;
                case AlreadyStarted(String eventId, LocalDateTime startsAt) -> eventId + " started at " + startsAt;
            };
            System.out.println(message);
        }
    }

    // Java 21: the first and the last element, the same way on every ordered collection.
    private static void sequencedCollections() {
        System.out.println("--- Java 21: sequenced collections");
        List<Event> events = Java8Features.events();
        System.out.println(events.get(0).getName() + " / " + events.get(events.size() - 1).getName());
        System.out.println(events.getFirst().getName() + " / " + events.getLast().getName());
        System.out.println(events.reversed().getFirst().getName());
    }

    // Java 21: 2,000 tasks that each wait 200 ms, as a database or HTTP call would.
    private static void virtualThreads() throws InterruptedException {
        System.out.println("--- Java 21: virtual threads");
        System.out.println("100 platform threads: " + runTasks(Executors.newFixedThreadPool(100)) + " ms");
        System.out.println("virtual threads:      " + runTasks(Executors.newVirtualThreadPerTaskExecutor()) + " ms");
    }

    private static long runTasks(ExecutorService executor) throws InterruptedException {
        Instant start = Instant.now();
        try (executor) {
            for (int i = 0; i < 2_000; i++) {
                executor.submit(() -> {
                    Thread.sleep(Duration.ofMillis(200));
                    return null;
                });
            }
        }
        return Duration.between(start, Instant.now()).toMillis();
    }
}
