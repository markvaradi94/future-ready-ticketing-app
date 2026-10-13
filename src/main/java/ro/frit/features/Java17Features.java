package ro.frit.features;

import ro.frit.ticketing.domain.BookingResult;
import ro.frit.ticketing.domain.Event;
import ro.frit.ticketing.domain.Venue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class Java17Features {
    public static void main(String[] args) {
        records();
        switchExpressions();
        sealedTypes();
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
}
