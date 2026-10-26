package ro.frit.ticketing.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.BookingSummaryResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.EventCancelled;
import ro.frit.ticketing.event.EventChanged;
import ro.frit.ticketing.event.EventNotFoundException;
import ro.frit.ticketing.event.EventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.MILLIS;
import static ro.frit.ticketing.booking.BookingCancelled.Reason.BY_CUSTOMER;
import static ro.frit.ticketing.booking.BookingCancelled.Reason.EVENT_CANCELLED;

/**
 * Books seats. The rules about the event itself (does it exist, has it started, are there seats) belong to
 * {@link EventService}; this service asks it to reserve the seats, then records who booked them.
 */
@Service
@RequiredArgsConstructor
public class BookingService {
    private final EventService eventService;
    private final BookingMapper bookingMapper;
    private final BookingRepository bookingRepository;
    private final BookingEventPublisher bookingEventPublisher;

    public BookingResponse book(String eventId, CreateBookingRequest request) {
        eventService.reserveSeats(eventId, request.seats());

        Booking booking = bookingMapper.toEntity(eventId, request, LocalDateTime.now().truncatedTo(MILLIS));
        Booking savedBooking = bookingRepository.save(booking);

        bookingEventPublisher.publish(BookingConfirmed.of(savedBooking));
        return bookingMapper.toResponse(savedBooking);
    }

    public void cancel(String eventId, String bookingId) {
        Booking booking = bookingRepository.findByIdAndEventId(bookingId, eventId)
                .orElseThrow(() -> new BookingNotFoundException(eventId, bookingId));

        eventService.releaseSeats(eventId, booking.getSeats());
        bookingRepository.delete(booking);
        bookingEventPublisher.publish(BookingCancelled.of(booking, BY_CUSTOMER));
    }

    // The event id must match too: otherwise a booking could be read through any event's URL.
    public BookingResponse findById(String eventId, String bookingId) {
        return bookingRepository.findByIdAndEventId(bookingId, eventId)
                .map(bookingMapper::toResponse)
                .orElseThrow(() -> new BookingNotFoundException(eventId, bookingId));
    }

    public List<BookingResponse> findAllFor(String eventId) {
        ensureEventExists(eventId);

        return bookingRepository.findByEventIdOrderByBookedAt(eventId).stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    public Map<String, Integer> bookedSeatsPerEvent() {
        return bookingRepository.sumSeatsPerEvent();
    }

    public BookingSummaryResponse summaryFor(String eventId) {
        ensureEventExists(eventId);

        return bookingRepository.summaryFor(eventId)
                .orElseGet(() -> BookingSummaryResponse.builder().eventId(eventId).build());
    }

    @EventListener
    public void onEventCancelled(EventCancelled cancelled) {
        List<Booking> bookings = bookingRepository.findByEventIdOrderByBookedAt(cancelled.eventId());

        bookingRepository.deleteAll(bookings);
        bookings.forEach(booking -> bookingEventPublisher.publish(BookingCancelled.of(booking, EVENT_CANCELLED)));
    }

    // Added after the session (lab round 3): the event changed in a way its customers care about, so each of them is
    // told, with their own booking.
    @EventListener
    public void onEventChanged(EventChanged changed) {
        bookingRepository.findByEventIdOrderByBookedAt(changed.eventId())
                .forEach(booking -> bookingEventPublisher.publish(BookedEventChanged.of(booking, changed)));
    }

    private void ensureEventExists(String eventId) {
        if (!eventService.existsById(eventId)) {
            throw new EventNotFoundException(eventId);
        }
    }
}