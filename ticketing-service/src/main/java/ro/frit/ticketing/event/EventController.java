package ro.frit.ticketing.event;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;
import ro.frit.ticketing.event.dto.EventSearchRequest;
import ro.frit.ticketing.event.dto.UpdateEventRequest;

import java.net.URI;
import java.util.List;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping
    List<EventResponse> search(@ParameterObject EventSearchRequest searchRequest) {
        return eventService.search(searchRequest);
    }

    @GetMapping("/top")
    List<EventResponse> findTop(@RequestParam(defaultValue = "3") @Positive int n) {
        return eventService.findTopByBookedSeats(n);
    }

    @GetMapping("/{id}")
    EventResponse findById(@PathVariable String id) {
        return eventService.findById(id);
    }

    @ApiResponse(responseCode = "201", description = "Created; the Location header points to the new event")
    @ApiResponse(responseCode = "400", description = "Invalid body: a field is missing, or the event is in the past",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        EventResponse created = eventService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    EventResponse update(@PathVariable String id, @Valid @RequestBody UpdateEventRequest request) {
        return eventService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    void delete(@PathVariable String id) {
        eventService.delete(id);
    }
}
