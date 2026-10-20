package ro.frit.ticketing.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.config.TicketingProperties.SeedEvent;
import ro.frit.ticketing.event.EventRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Persistence test ({@link MongoPersistenceTest}): the seeder fills an empty collection, and
 * leaves a filled one alone, so a restart keeps the booked seats.
 */
@MongoPersistenceTest
class DataSeederTest {

    private static final TicketingProperties PROPERTIES = new TicketingProperties(List.of(
            new SeedEvent("rock-cluj", "Rock Night", "BT Arena", "Cluj", LocalDateTime.of(2030, 6, 12, 20, 0), 500)));

    @Autowired
    private EventRepository eventRepository;

    private DataSeeder seeder;

    @BeforeEach
    void startEmpty() {
        eventRepository.deleteAll();
        seeder = new DataSeeder(PROPERTIES, eventRepository);
    }

    @Test
    void seedsAnEmptyCollection() {
        seeder.run(null);

        assertEquals(500, eventRepository.findById("rock-cluj").orElseThrow().getCapacity());
    }

    @Test
    void aRestartKeepsTheBookedSeats() {
        seeder.run(null);
        eventRepository.save(eventRepository.findById("rock-cluj").orElseThrow().toBuilder().bookedSeats(2).build());

        seeder.run(null);

        assertEquals(2, eventRepository.findById("rock-cluj").orElseThrow().getBookedSeats());
    }
}
