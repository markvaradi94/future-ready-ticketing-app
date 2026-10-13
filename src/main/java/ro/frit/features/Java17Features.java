package ro.frit.features;

import ro.frit.ticketing.domain.BookingRequest;
import ro.frit.ticketing.domain.BookingResult;
import ro.frit.ticketing.domain.Event;
import ro.frit.ticketing.domain.Venue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Java17Features {
    public static void main(String[] args) {
        records();
        switchExpressions();
        sealedTypes();
        helpfulNullPointerExceptions();
        textBlocks();
        recordsInDepth();
        entities();
        instanceofPatterns();
        streamToList();
    }

    static List<Event> events() {
        return Arrays.asList(
                new Event("rock-cluj", "Rock Night", new Venue("BT Arena", "Cluj"), LocalDateTime.of(2030, 6, 12, 20, 0), 500, 480),
                new Event("jazz-cluj", "Jazz in the Park", new Venue("Central Park", "Cluj"), LocalDateTime.of(2030, 7, 3, 19, 0), 200, 200),
                new Event("opera-iasi", "La Traviata", new Venue("National Opera", "Iasi"), LocalDateTime.of(2030, 9, 20, 18, 30), 350, 100),
                new Event("electro-buc", "Electronic Night", new Venue("Form Space", "Bucuresti"), LocalDateTime.of(2030, 10, 10, 22, 0), 800, 0));
    }

    private static void sealedTypes() {
        System.out.println("---- SEALED TYPES ----");

        BookingResult result = new BookingResult.SoldOut("rock-cluj", 2);

        switch (result) {
            case BookingResult.SoldOut ignored -> System.out.println("SOLD OUT");
            case BookingResult.AlreadyStarted ignored -> System.out.println("ALREADY STARTED");
            case BookingResult.UnknownEvent ignored -> System.out.println("UNKNOWN EVENT");
            case BookingResult.Confirmed ignored -> System.out.println("CONFIRMED");
        }
    }

    private static void switchExpressions() {
        System.out.println("---- SWITCH EXPRESSIONS ----");

        String category = "vip";
        int oldPrice;

        switch (category) {
            case "vip":
                oldPrice = 100;
                break;
            case "student":
            case "child":
                oldPrice = 50;
                break;
            default:
                oldPrice = 10;
        }

        int newPrice = switch (category) {
            case "vip" -> 100;
            case "student", "child" -> 50;
            default -> 10;
        };

        System.out.println(oldPrice + " -> " + newPrice);
    }

    private static void records() {
        System.out.println("---- RECORDS ----");
        Venue venue1 = new Venue("BT Arena", "Cluj");
        Venue venue2 = new Venue("BT Arena", "Cluj");

        System.out.println(venue1.equals(venue2));
    }

    // Added after the session. Java 14: the message says which variable was null.
    private static void helpfulNullPointerExceptions() {
        System.out.println("---- HELPFUL NULL POINTER EXCEPTIONS ----");

        Event event = events().get(0);
        String city = null;
        try {
            System.out.println(event.getName().length() + city.length());
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        }
    }

    // Added after the session. Java 15: a string over several lines, as it looks.
    private static void textBlocks() {
        System.out.println("---- TEXT BLOCKS ----");

        String json = """
                {
                  "customerEmail": "ana@mail.ro",
                  "seats": 2
                }
                """;
        System.out.print(json);
    }

    // Added after the session. Java 16: two more things about records.
    private static void recordsInDepth() {
        System.out.println("---- RECORDS IN DEPTH ----");

        // The compact constructor runs on every creation, so an invalid request cannot exist.
        try {
            new BookingRequest("rock-cluj", "ana@mail.ro", 0);
        } catch (IllegalArgumentException e) {
            System.out.println("an invalid request cannot exist: " + e.getMessage());
        }

        // The surprise: a record's fields cannot be reassigned, but what they point to can still change.
        List<String> artists = new ArrayList<>(List.of("Ana", "Ion"));
        Lineup lineup = new Lineup(artists);
        artists.add("a surprise guest");
        System.out.println(lineup);

        SafeLineup safe = new SafeLineup(artists);
        artists.add("another one");
        System.out.println(safe);
    }

    record Lineup(List<String> artists) {
    }

    record SafeLineup(List<String> artists) {
        SafeLineup {
            artists = List.copyOf(artists);
        }
    }

    // Added after the session. Not a Java feature, a modelling rule:
    // an entity is equal by its id, a record by everything it holds.
    private static void entities() {
        System.out.println("---- ENTITY OR VALUE? ----");

        Event event = events().get(0);
        Event afterABooking = event.toBuilder()
                .bookedSeats(event.getBookedSeats() + 2)
                .build();
        System.out.println(event.getBookedSeats() + " -> " + afterABooking.getBookedSeats());
        System.out.println("still the same event: " + event.equals(afterABooking));
    }

    // Added after the session. Java 16: check and cast in one step.
    private static void instanceofPatterns() {
        System.out.println("---- PATTERNS FOR INSTANCEOF ----");

        Object obj = new Venue("Central Park", "Cluj");

        if (obj instanceof Venue) {
            Venue venue = (Venue) obj;
            System.out.println(venue.city());
        }

        if (obj instanceof Venue venue) {
            System.out.println(venue.city());
        }
    }

    // Added after the session. Java 16: toList() on a stream, unmodifiable.
    private static void streamToList() {
        System.out.println("---- STREAM.TOLIST ----");

        List<String> names = events().stream()
                .map(Event::getName)
                .toList();
        System.out.println(names);
    }
}
