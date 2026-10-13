package ro.frit.features;

import ro.frit.ticketing.domain.Event;
import ro.frit.ticketing.domain.Venue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Java8Features {
    public static void main(String[] args) {
        lambdas();
        streams();
        optional();
        moreStreams();
        methodReferences();
        timeApi();
    }

    static List<Event> events() {
        return Arrays.asList(
                new Event("rock-cluj", "Rock Night", new Venue("BT Arena", "Cluj"), LocalDateTime.of(2030, 6, 12, 20, 0), 500, 480),
                new Event("jazz-cluj", "Jazz in the Park", new Venue("Central Park", "Cluj"), LocalDateTime.of(2030, 7, 3, 19, 0), 200, 200),
                new Event("opera-iasi", "La Traviata", new Venue("National Opera", "Iasi"), LocalDateTime.of(2030, 9, 20, 18, 30), 350, 100),
                new Event("electro-buc", "Electronic Night", new Venue("Form Space", "Bucuresti"), LocalDateTime.of(2030, 10, 10, 22, 0), 800, 0));
    }

    private static void optional() {
        System.out.println("---- OPTIONAL ----");

        List<Event> events = events();
        Event found = null;

        for (Event event : events) {
            if (event.getId().equalsIgnoreCase("non-existing")) {
                found = event;
                break;
            }
        }

        if (found == null) {
            System.out.println("Event not found");
        } else {
            System.out.println(found.getName());
        }



        Optional<Event> optionalEvent = events().stream()
                .filter(event -> event.getId().equalsIgnoreCase("non-existing"))
                .findAny();

        if (optionalEvent.isPresent()) {
            Event event = optionalEvent.get();
            System.out.println(event.getName());
        } else {
            System.out.println("Event not found");
        }

//        System.out.println(optionalEvent.get().getName());

        Event event1 = events.stream()
                .filter(event -> event.getId().equalsIgnoreCase("rock-cluj"))
                .findAny()
                .orElse(defaultEvent());

        Event event2 = events.stream()
                .filter(event -> event.getId().equalsIgnoreCase("rock-cluj"))
                .findAny()
                .orElseGet(Java8Features::defaultEvent);

        System.out.println(event1.getName());
        System.out.println(event2.getName());

        var var = 2;
    }

    private static Event defaultEvent() {
        System.out.println("TIME CONSUMING OPERATION");
        return new Event("non-existing", "Non-existing", new Venue("Non-existing", "Non-existing"), LocalDateTime.now(), 0, 0);
    }

    private static void lambdas() {
        System.out.println("---- LAMBDAS ----");

        List<Event> events = events();

        Predicate<Event> moreThan200Bookings = new Predicate<Event>() {
            @Override
            public boolean test(Event event) {
                return event.getBookedSeats() >= 200;
            }
        };

        List<Event> filtered = new ArrayList<>();
        for (Event event : events) {

            if (moreThan200Bookings.test(event)) {
                filtered.add(event);
            }
        }

        filtered.forEach(System.out::println);

        System.out.println("---- JAVA 8+ ----");

        List<Event> collect = events.stream()
                .filter(event -> event.getBookedSeats() >= 200)
                .collect(Collectors.toList());

        collect.forEach(System.out::println);
    }

    private static void streams() {
        System.out.println("---- STREAMS ----");

        List<String> events = events().stream()
                .filter(event -> event.getCapacity() > 300)
                .map(Event::getName)
                .sorted()
                .collect(Collectors.toList());

        events.forEach(System.out::println);
    }

    // Added after the session: grouping, a sum, and a yes/no question on a stream.
    private static void moreStreams() {
        System.out.println("---- MORE STREAMS ----");

        Map<String, List<String>> namesByCity = events().stream()
                .collect(Collectors.groupingBy(
                        event -> event.getVenue().city(),
                        Collectors.mapping(Event::getName, Collectors.toList())));
        System.out.println(namesByCity);

        int bookedInTotal = events().stream()
                .mapToInt(Event::getBookedSeats)
                .sum();
        System.out.println("booked in total: " + bookedInTotal);

        boolean anySoldOut = events().stream().anyMatch(event -> event.availableSeats() == 0);
        System.out.println("any sold out: " + anySoldOut);
    }

    // Added after the session: a method reference is a lambda that only calls one method.
    private static void methodReferences() {
        System.out.println("---- METHOD REFERENCES ----");

        Function<Event, String> anyInstance = Event::getName;            // event -> event.getName()
        Function<Integer, String> staticMethod = String::valueOf;        // n -> String.valueOf(n)
        Supplier<List<Event>> constructor = ArrayList::new;              // () -> new ArrayList<>()

        System.out.println(anyInstance.apply(events().get(0)) + " " + staticMethod.apply(42) + " " + constructor.get());
        events().stream().map(Event::getId).forEach(System.out::println);  // id -> System.out.println(id)
    }

    // Added after the session: java.time has immutable dates and times, with one type for each idea.
    private static void timeApi() {
        System.out.println("---- JAVA.TIME ----");

        LocalDateTime sessionOne = LocalDateTime.of(2026, 10, 12, 18, 0);
        LocalDateTime rockNight = events().get(0).getStartsAt();
        System.out.println(sessionOne.getDayOfWeek() + ", ends at " + sessionOne.plusHours(3).toLocalTime());
        System.out.println("days until Rock Night: " + Duration.between(sessionOne, rockNight).toDays());
        System.out.println("already started: " + rockNight.isBefore(sessionOne));
    }
}
