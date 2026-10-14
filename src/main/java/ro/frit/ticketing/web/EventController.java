package ro.frit.ticketing.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ro.frit.ticketing.service.TicketOffice;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final TicketOffice ticketOffice;

    @GetMapping
    public List<EventResponse> allEvents() {
        return ticketOffice.allEvents().stream()
                .map(EventResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public EventResponse event(@PathVariable String id) {
        return ticketOffice.findEvent(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No event with id " + id));
    }
}
