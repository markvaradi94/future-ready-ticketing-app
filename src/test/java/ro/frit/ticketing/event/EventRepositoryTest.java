package ro.frit.ticketing.event;

import jakarta.validation.ConstraintViolationException;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.frit.ticketing.MongoPersistenceTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persistence test ({@link MongoPersistenceTest}): how an {@link Event} is stored, and that an
 * invalid one is never stored.
 */
@MongoPersistenceTest
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void emptyTheCollection() {
        eventRepository.deleteAll();
    }

    @Test
    void theVenueIsEmbeddedInTheEventDocument() {
        eventRepository.save(event("e1", "Concert"));

        Document stored = mongoTemplate.findById("e1", Document.class, "events");

        assertEquals("Concert", stored.getString(Event.Fields.name));
        assertEquals("Cluj", stored.get(Event.Fields.venue, Document.class).getString(Venue.Fields.city));
    }

    @Test
    void invalidEventIsNotSaved() {
        assertThrows(ConstraintViolationException.class, () -> eventRepository.save(event("e1", " ")));

        assertTrue(eventRepository.findById("e1").isEmpty());
    }

    private static Event event(String id, String name) {
        return Event.builder()
                .id(id)
                .name(name)
                .venue(new Venue("Arena", "Cluj"))
                .startsAt(LocalDateTime.now().plusDays(7))
                .capacity(100)
                .build();
    }
}
