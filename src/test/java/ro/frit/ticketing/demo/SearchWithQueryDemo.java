package ro.frit.ticketing.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.event.Event;
import ro.frit.ticketing.event.EventRepository;
import ro.frit.ticketing.event.Venue;
import ro.frit.ticketing.event.dto.EventSearchRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * Session 4 demo, step 1: the event search written three ways, run on the same four events (Rock Night in Cluj, Jazz
 * Evening in Cluj, Rock Opera in Iasi, Rock Night in Cluj-Napoca). It prints what each call returned and asserts
 * nothing.
 *
 * <pre>./gradlew test --tests '*SearchWithQueryDemo' -i</pre>
 *
 * Look for the {@code QUERY} lines, and compare:
 * <ul>
 *     <li>{@code plain @Query}: a filter that was not sent still filters (no dates: nothing found; no city: an
 *     exception), {@code Cluj} also finds Cluj-Napoca, and {@code .} matches every city;</li>
 *     <li>{@code SpEL @Query}: a filter that was not sent is left out, but the city is still a regex;</li>
 *     <li>{@code MongoTemplate}: the application's search, built in Java from the filters that were sent.</li>
 * </ul>
 */
@Tag("demo")
@MongoPersistenceTest
class SearchWithQueryDemo {

    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 1, 12, 0);
    private static final LocalDateTime NEXT_YEAR = NOW.plusYears(1);

    // The two @Query versions, kept here so the application's repository does not carry them.
    interface QuerySearches extends MongoRepository<Event, String> {

        // The search as one MongoDB filter: ?0 to ?3 are the parameters, in order.
        @Query(value = "{ 'venue.city': { $regex: ?0, $options: 'i' }, name: { $regex: ?1, $options: 'i' }, startsAt: { $gte: ?2, $lt: ?3 } }",
                sort = "{ startsAt: 1 }")
        List<Event> plain(String city, String name, LocalDateTime from, LocalDateTime to);

        // ?#{ … } is SpEL, evaluated in Java before the query is sent; [0] to [3] are the parameters. A missing value
        // becomes a condition every document meets (each has an _id), because a string cannot leave a slot out.
        @Query(value = "{ $and: [ "
                + "?#{ [0] == null ? { _id: { $exists: true } } : { 'venue.city': { $regex: [0], $options: 'i' } } }, "
                + "?#{ [1] == null ? { _id: { $exists: true } } : { 'name': { $regex: [1], $options: 'i' } } }, "
                + "?#{ [2] == null ? { _id: { $exists: true } } : { 'startsAt': { $gte: [2] } } }, "
                + "?#{ [3] == null ? { _id: { $exists: true } } : { 'startsAt': { $lt: [3] } } } ] }",
                sort = "{ startsAt: 1 }")
        List<Event> withSpel(String city, String name, LocalDateTime from, LocalDateTime to);
    }

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private QuerySearches querySearches;

    @BeforeEach
    void storeFourEvents() {
        // Spring Data builds the repository from the interface, as it does for EventRepository at startup.
        querySearches = new MongoRepositoryFactory(mongoTemplate).getRepository(QuerySearches.class);

        eventRepository.deleteAll();
        eventRepository.save(event("e1", "Rock Night", "Cluj", NOW.plusDays(10)));
        eventRepository.save(event("e2", "Jazz Evening", "Cluj", NOW.plusDays(20)));
        eventRepository.save(event("e3", "Rock Opera", "Iasi", NOW.plusDays(30)));
        eventRepository.save(event("e4", "Rock Night", "Cluj-Napoca", NOW.plusDays(40)));
    }

    @Test
    void plainQuery() {
        print("plain @Query", "all four filters, city Cluj", () -> querySearches.plain("Cluj", "rock", NOW, NEXT_YEAR));
        print("plain @Query", "no dates sent", () -> querySearches.plain("Cluj", "rock", null, null));
        print("plain @Query", "no city sent", () -> querySearches.plain(null, "rock", NOW, NEXT_YEAR));
        print("plain @Query", "city '.'", () -> querySearches.plain(".", "rock", NOW, NEXT_YEAR));
    }

    @Test
    void spelQuery() {
        print("SpEL @Query", "all four filters, city Cluj", () -> querySearches.withSpel("Cluj", "rock", NOW, NEXT_YEAR));
        print("SpEL @Query", "no dates sent", () -> querySearches.withSpel("Cluj", "rock", null, null));
        print("SpEL @Query", "no city sent", () -> querySearches.withSpel(null, "rock", NOW, NEXT_YEAR));
        print("SpEL @Query", "city '.'", () -> querySearches.withSpel(".", "rock", NOW, NEXT_YEAR));
    }

    @Test
    void mongoTemplateSearch() {
        print("MongoTemplate", "all four filters, city Cluj",
                () -> eventRepository.search(new EventSearchRequest("Cluj", "rock", NOW, NEXT_YEAR)));
        print("MongoTemplate", "no dates sent",
                () -> eventRepository.search(new EventSearchRequest("Cluj", "rock", null, null)));
        print("MongoTemplate", "no city sent",
                () -> eventRepository.search(new EventSearchRequest(null, "rock", NOW, NEXT_YEAR)));
        print("MongoTemplate", "city '.'",
                () -> eventRepository.search(new EventSearchRequest(".", "rock", NOW, NEXT_YEAR)));
    }

    private static void print(String version, String call, Supplier<List<Event>> search) {
        String result;
        try {
            result = search.get().stream()
                    .map(event -> event.getName() + " in " + event.getVenue().city())
                    .toList()
                    .toString();
        } catch (RuntimeException e) {
            result = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        System.out.println("QUERY " + version + ", " + call + ": " + result);
    }

    private static Event event(String id, String name, String city, LocalDateTime startsAt) {
        return Event.builder()
                .id(id)
                .name(name)
                .venue(new Venue("Arena", city))
                .startsAt(startsAt)
                .capacity(100)
                .build();
    }
}
