package ro.frit.ticketing.event;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.TestcontainersConfiguration;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

// Hibernate checks the entity's constraints before it writes: an invalid event is never inserted.
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Test
    void invalidEventIsNotSaved() {
        Event invalid = Event.builder()
                .id("e1")
                .name(" ")
                .venue(new Venue("Arena", "Cluj"))
                .startsAt(LocalDateTime.now().plusDays(7))
                .capacity(100)
                .build();

        assertThrows(ConstraintViolationException.class, () -> eventRepository.saveAndFlush(invalid));
    }
}
