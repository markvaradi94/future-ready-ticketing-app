package ro.frit.features;

import ro.frit.ticketing.domain.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Java 9 (2017), 10 (2018) and 11 (2018, LTS): collection factories, Optional additions, var, String helpers.
 */
public class Java11Features {

    public static void main(String[] args) {
        collectionFactories();
        optionalAdditions();
        localVariableTypeInference();
        strings();
    }

    // Java 9: an immutable collection in one call.
    private static void collectionFactories() {
        System.out.println("--- Java 9: List.of, Map.of");
        List<String> cities = List.of("Cluj", "Iasi", "Bucuresti");
        Map<String, Integer> capacities = Map.of("rock-cluj", 500, "jazz-cluj", 200);
        System.out.println(cities + " " + capacities.get("rock-cluj"));

        try {
            cities.add("Oradea");
        } catch (UnsupportedOperationException e) {
            System.out.println("List.of is immutable: add() throws " + e.getClass().getSimpleName());
        }
    }

    // Java 9: both branches in one call. Java 10: orElseThrow() with no argument, the honest name for get().
    private static void optionalAdditions() {
        System.out.println("--- Java 9 and 10: Optional");
        Optional<String> found = Optional.of("Rock Night");
        Optional<String> missing = Optional.empty();

        found.ifPresentOrElse(name -> System.out.println("found " + name), () -> System.out.println("no event"));
        missing.ifPresentOrElse(name -> System.out.println("found " + name), () -> System.out.println("no event"));
        System.out.println(found.orElseThrow());
    }

    // Java 10: var. The type is still fixed at compile time; it is only not written twice.
    private static void localVariableTypeInference() {
        System.out.println("--- Java 10: var");
        var events = new ArrayList<Event>();
        events.addAll(Java8Features.events());
        // events.add("a string");   // does not compile: events is an ArrayList<Event>
        var first = events.get(0);
        System.out.println(first.getName());

        var var = 2;                 // legal: var is a reserved type name, not a keyword
        System.out.println(var);

        var copy = List.copyOf(events);
        System.out.println("List.copyOf is immutable too: " + copy.size() + " events");
    }

    // Java 11: the String methods everyone used to write by hand.
    private static void strings() {
        System.out.println("--- Java 11: String");
        String email = "   ";
        System.out.println(email.isEmpty() + " " + email.isBlank());
        System.out.println("[" + "  ana@mail.ro  ".strip() + "]");
        System.out.println("=".repeat(30));
        System.out.println("rock\njazz\nopera".lines().count() + " lines");
    }
}
