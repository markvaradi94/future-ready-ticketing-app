package ro.frit.ticketing.booking;

import ro.frit.ticketing.booking.dto.BookingSummaryResponse;

import java.util.Map;
import java.util.Optional;

public interface BookingReportRepository {

    Map<String, Integer> sumSeatsPerEvent();

    // Empty when the event has no bookings.
    Optional<BookingSummaryResponse> summaryFor(String eventId);
}