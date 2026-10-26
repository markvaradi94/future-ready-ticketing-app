package ro.frit.ticketing.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ro.frit.ticketing.config.TicketingProperties.SeedEvent;
import ro.frit.ticketing.event.Event;
import ro.frit.ticketing.event.EventRepository;
import ro.frit.ticketing.event.Venue;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {
    private final TicketingProperties ticketingProperties;
    private final EventRepository eventRepository;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (eventRepository.count() > 0) {
            log.info("Events already stored, nothing to seed");
            return;
        }
        ticketingProperties.seedEvents().stream()
                .map(DataSeeder::toEvent)
                .forEach(eventRepository::save);
        log.info("Seeded {} events", ticketingProperties.seedEvents().size());
    }

    private static Event toEvent(SeedEvent seedEvent) {
        return Event.builder()
                .id(seedEvent.id())
                .name(seedEvent.name())
                .venue(new Venue(seedEvent.venueName(), seedEvent.city()))
                .startsAt(seedEvent.startsAt())
                .capacity(seedEvent.capacity())
                .bookedSeats(0)
                .build();
    }
}
