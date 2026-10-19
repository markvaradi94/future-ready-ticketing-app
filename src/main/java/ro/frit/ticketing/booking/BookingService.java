package ro.frit.ticketing.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.EventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

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

        Booking booking = bookingMapper.toEntity(
                UUID.randomUUID().toString(), eventId, request, LocalDateTime.now());
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    // The event id must match too: otherwise a booking could be read through any event's URL.
    public BookingResponse findById(String eventId, String bookingId) {
        return bookingRepository.findById(bookingId)
                .filter(booking -> booking.getEventId().equals(eventId))
                .map(bookingMapper::toResponse)
                .orElseThrow(() -> new BookingNotFoundException(eventId, bookingId));
    }

    // N+1: one select for the bookings, then one more for each event, the first time getEvent() is used.
    @Transactional(readOnly = true)
    public Map<String, Integer> bookedSeatsPerEventName() {
        return seatsPerEventName(bookingRepository.findAll());
    }

    // The fix: one select with a join.
    @Transactional(readOnly = true)
    public Map<String, Integer> bookedSeatsPerEventNameFetchJoin() {
        return seatsPerEventName(bookingRepository.findAllWithEvent());
    }

    private static Map<String, Integer> seatsPerEventName(List<Booking> bookings) {
        return bookings.stream()
                .collect(Collectors.groupingBy(booking -> booking.getEvent().getName(), TreeMap::new,
                        Collectors.summingInt(Booking::getSeats)));
    }

    public Map<String, Integer> bookedSeatsPerEvent() {
        return bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(Booking::getEventId, Collectors.summingInt(Booking::getSeats)));
    }
}
