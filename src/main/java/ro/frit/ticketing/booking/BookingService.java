package ro.frit.ticketing.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.BookingSummaryResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.EventNotFoundException;
import ro.frit.ticketing.event.EventService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * Books seats. The rules about the event itself (does it exist, has it started, are there seats) belong to
 * {@link EventService}; this service asks it to reserve the seats, then records who booked them.
 */
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final EventService eventService;

    public BookingResponse book(String eventId, CreateBookingRequest request) {
        eventService.reserveSeats(eventId, request.seats());

        Booking booking = bookingMapper.toEntity(eventId, request, LocalDateTime.now().truncatedTo(MILLIS));
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    // The event id must match too: otherwise a booking could be read through any event's URL.
    public BookingResponse findById(String eventId, String bookingId) {
        return bookingRepository.findById(bookingId)
                .filter(booking -> booking.getEventId().equals(eventId))
                .map(bookingMapper::toResponse)
                .orElseThrow(() -> new BookingNotFoundException(eventId, bookingId));
    }

    public Map<String, Integer> bookedSeatsPerEvent() {
        return bookingRepository.sumSeatsPerEvent();
    }

    public BookingSummaryResponse summaryFor(String eventId) {
        if (!eventService.existsById(eventId)) {
            throw new EventNotFoundException(eventId);
        }

        return bookingRepository.summaryFor(eventId)
                .orElseGet(() -> BookingSummaryResponse.builder().eventId(eventId).build());
    }
}
