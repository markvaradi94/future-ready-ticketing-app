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
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Gatherers;

/**
 * Java 22 to 25 (2025, LTS): unnamed variables (22), stream gatherers (24), flexible constructor bodies (25).
 */
public class Java25Features {

    public static void main(String[] args) {
        unnamedVariables();
        streamGatherers();
        moreGatherers();
        flexibleConstructorBodies();
    }

    // Java 22: an underscore for a value you do not use.
    private static void unnamedVariables() {
        System.out.println("--- Java 22: unnamed variables");
        BookingResult result = new SoldOut("rock-cluj", 20);
        int seatsLeft = switch (result) {
            case Confirmed(Booking _) -> -1;
            case SoldOut(_, int availableSeats) -> availableSeats;
            case UnknownEvent(_) -> -1;
            case AlreadyStarted _ -> -1;
        };
        System.out.println(seatsLeft);

        try {
            Integer.parseInt("two seats");
        } catch (NumberFormatException _) {
            System.out.println("not a number");
        }
    }

    // Java 24: your own step in the middle of a stream. The JDK brings a few; these two cut a stream into windows.
    private static void streamGatherers() {
        System.out.println("--- Java 24: stream gatherers");
        List<Event> events = Java8Features.events();

        // windowFixed: groups with no overlap. Use it to work in batches, such as 3 reminder emails per call.
        List<List<String>> batches = events.stream()
                .map(Event::getName)
                .gather(Gatherers.windowFixed(3))
                .toList();
        System.out.println("batches of 3: " + batches);

        // windowSliding: every element together with the next one. Use it to compare neighbours.
        System.out.println("days between one event and the next:");
        events.stream()
                .sorted(Comparator.comparing(Event::getStartsAt))
                .gather(Gatherers.windowSliding(2))
                .map(pair -> "  %s -> %s: %d days".formatted(
                        pair.getFirst().getName(),
                        pair.getLast().getName(),
                        ChronoUnit.DAYS.between(pair.getFirst().getStartsAt().toLocalDate(), pair.getLast().getStartsAt().toLocalDate())))
                .forEach(System.out::println);
    }

    // The other three ready-made gatherers.
    private static void moreGatherers() {
        System.out.println("--- Java 24: more gatherers");
        List<Event> events = Java8Features.events();

        // scan: a running result, one output for each input. Here: seats sold so far, event by event.
        List<Integer> runningTotal = events.stream()
                .map(Event::getBookedSeats)
                .gather(Gatherers.scan(() -> 0, Integer::sum))
                .toList();
        System.out.println("seats sold so far: " + runningTotal);

        // fold: the same, but only the last result comes out, as a stream of one element.
        List<Integer> total = events.stream()
                .map(Event::getBookedSeats)
                .gather(Gatherers.fold(() -> 0, Integer::sum))
                .toList();
        System.out.println("seats sold in all: " + total);

        // mapConcurrent: map, with several calls running at once on virtual threads. The order is kept.
        Instant start = Instant.now();
        List<String> prices = events.stream()
                .gather(Gatherers.mapConcurrent(4, Java25Features::slowPriceLookup))
                .toList();
        System.out.println(prices + " in " + Duration.between(start, Instant.now()).toMillis() + " ms");
    }

    // Stands in for a call to another service that takes 300 ms.
    private static String slowPriceLookup(Event event) {
        try {
            Thread.sleep(Duration.ofMillis(300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return event.getName() + ": " + (event.getCapacity() / 10) + " lei";
    }

    // Java 25: statements before super(), so an invalid object never reaches the parent constructor.
    private static void flexibleConstructorBodies() {
        System.out.println("--- Java 25: flexible constructor bodies");
        try {
            new Festival("", 3);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        System.out.println(new Festival("Untold", 4).describe());
    }

    static class Show {
        private final String name;

        Show(String name) {
            this.name = name;
        }

        String describe() {
            return name;
        }
    }

    static class Festival extends Show {
        private final int days;

        Festival(String name, int days) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("A festival needs a name");
            }
            super(name);
            this.days = days;
        }

        @Override
        String describe() {
            return super.describe() + ", " + days + " days";
        }
    }
}
