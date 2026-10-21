package ro.frit.ticketing.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;
import ro.frit.ticketing.event.dto.EventSearchRequest;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping
    public List<EventResponse> search(EventSearchRequest searchRequest) {
        return eventService.search(searchRequest);
    }

    @GetMapping("/top")
    public List<EventResponse> findTop(@RequestParam(defaultValue = "3") @Positive int n) {
        return eventService.findTopByBookedSeats(n);
    }

    @GetMapping("/{id}")
    public EventResponse findById(@PathVariable String id) {
        return eventService.findById(id);
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        EventResponse created = eventService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
