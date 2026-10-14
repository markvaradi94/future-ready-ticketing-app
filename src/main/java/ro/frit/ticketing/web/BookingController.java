package ro.frit.ticketing.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.frit.ticketing.domain.BookingRequest;
import ro.frit.ticketing.domain.BookingResult;
import ro.frit.ticketing.service.TicketOffice;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/events/{eventId}/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final TicketOffice ticketOffice;

    @PostMapping
    public ResponseEntity<?> book(@PathVariable String eventId, @RequestBody BookingRequest body) {
        BookingRequest request = body.toBuilder().eventId(eventId).build();

        return switch (ticketOffice.book(request)) {
            case BookingResult.Confirmed(var booking) -> ResponseEntity.status(CREATED).body(BookingResponse.from(booking));
            case BookingResult.UnknownEvent(var id) ->
                    ResponseEntity.of(ProblemDetail.forStatusAndDetail(NOT_FOUND, "No event with id " + id)).build();
            case BookingResult.SoldOut(var id, var availableSeats) -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(CONFLICT, "Event " + id + " is sold out");
                problem.setProperty("availableSeats", availableSeats);
                yield ResponseEntity.of(problem).build();
            }
            case BookingResult.AlreadyStarted(var id, var startsAt) -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(CONFLICT, "Event " + id + " has already started");
                problem.setProperty("startsAt", startsAt);
                yield ResponseEntity.of(problem).build();
            }
        };
    }
}