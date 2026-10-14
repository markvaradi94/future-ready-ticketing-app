package ro.frit.ticketing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ro.frit.ticketing.config.TicketingProperties.SeedEvent;
import ro.frit.ticketing.domain.Event;
import ro.frit.ticketing.domain.Venue;
import ro.frit.ticketing.service.TicketOffice;

import java.util.List;

@Configuration
public class TicketingConfig {

    @Bean
    public TicketOffice ticketOffice(TicketingProperties ticketingProperties) {
        List<Event> events = ticketingProperties.seedEvents().stream()
                .map(TicketingConfig::toEvent)
                .toList();

        System.out.printf(">>>> Spring is creating TicketOffice with %s events%n", events.size());
        return new TicketOffice(events);
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
