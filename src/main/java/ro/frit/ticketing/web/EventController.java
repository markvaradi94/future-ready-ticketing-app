package ro.frit.ticketing.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ro.frit.ticketing.domain.Event;
import ro.frit.ticketing.service.TicketOffice;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final TicketOffice ticketOffice;

    // Added after the session (stretch 1): ?city=cluj lists the upcoming events in a city.
    @GetMapping
    public List<EventResponse> allEvents(@RequestParam(required = false) String city) {
        List<Event> events = city == null || city.isBlank()
                ? ticketOffice.allEvents()
                : ticketOffice.upcomingEventsIn(city, LocalDateTime.now());
        return toResponses(events);
    }

    // Added after the session (stretch 3): /top is more specific than /{id}, so Spring picks this method for it.
    @GetMapping("/top")
    public List<EventResponse> topEvents(@RequestParam(defaultValue = "3") @Positive int n) {
        return toResponses(ticketOffice.topEventsByBookedSeats(n));
    }

    @GetMapping("/{id}")
    public EventResponse event(@PathVariable String id) {
        return ticketOffice.findEvent(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No event with id " + id));
    }

    // Added after the session (stretch 4): 201 with a Location header that points to the new event.
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest body) {
        Event event = ticketOffice.addEvent(body.toDomain(UUID.randomUUID().toString()));
        return ResponseEntity.created(URI.create("/events/" + event.getId()))
                .body(EventResponse.from(event));
    }

    private static List<EventResponse> toResponses(List<Event> events) {
        return events.stream()
                .map(EventResponse::from)
                .toList();
    }
}
