package ro.frit.ticketing.booking;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.BookingSummaryResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;

import java.net.URI;
import java.util.List;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/events/{eventId}/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Booked; the Location header points to the new booking")
    @ApiResponse(responseCode = "400", description = "Invalid body: no valid email, or seats not positive",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No such event",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Sold out, already started, or someone else booked at the same time",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<BookingResponse> book(@PathVariable String eventId, @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse bookingResponse = bookingService.book(eventId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{bookingId}")
                .buildAndExpand(bookingResponse.id())
                .toUri();
        return ResponseEntity.created(location).body(bookingResponse);
    }

    @GetMapping("/summary")
    @ApiResponse(responseCode = "200", description = "The event's booking summary")
    @ApiResponse(responseCode = "404", description = "No such event",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public BookingSummaryResponse summary(@PathVariable String eventId) {
        return bookingService.summaryFor(eventId);
    }

    @GetMapping("/{bookingId}")
    @ApiResponse(responseCode = "200", description = "The booking")
    @ApiResponse(responseCode = "404", description = "No such booking for this event",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public BookingResponse findById(@PathVariable String eventId, @PathVariable String bookingId) {
        return bookingService.findById(eventId, bookingId);
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "The event's bookings, oldest first")
    @ApiResponse(responseCode = "404", description = "No such event",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public List<BookingResponse> findAll(@PathVariable String eventId) {
        return bookingService.findAllFor(eventId);
    }

    @DeleteMapping("/{bookingId}")
    @ResponseStatus(NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Cancelled; the seats go back to the event")
    @ApiResponse(responseCode = "404", description = "No such booking for this event",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The event has already started",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public void delete(@PathVariable String eventId, @PathVariable String bookingId) {
        bookingService.cancel(eventId, bookingId);
    }
}