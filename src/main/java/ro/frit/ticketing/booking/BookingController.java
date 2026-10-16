package ro.frit.ticketing.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/events/{eventId}/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(CREATED)
    public BookingResponse book(@PathVariable String eventId, @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.book(eventId, request);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse findById(@PathVariable String eventId, @PathVariable String bookingId) {
        return bookingService.findById(eventId, bookingId);
    }
}
