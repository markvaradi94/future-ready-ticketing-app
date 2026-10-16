package ro.frit.ticketing.event;

import jakarta.validation.Valid;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps the events in memory for now. The method names are the ones Spring Data uses, so in Session 3 this class
 * becomes a {@code MongoRepository} interface and the service does not notice.
 * <p>
 * {@code @Validated} makes Spring check the {@code @Valid} parameter: saving an {@link Event} that breaks one of its
 * constraints throws a {@code ConstraintViolationException}, and nothing is stored.
 */
@Repository
@Validated
public class EventRepository {
    private final Map<String, Event> events = new LinkedHashMap<>();

    public Event save(@Valid Event event) {
        events.put(event.getId(), event);
        return event;
    }

    public Optional<Event> findById(String id) {
        return Optional.ofNullable(events.get(id));
    }

    public List<Event> findAll() {
        return List.copyOf(events.values());
    }
}
