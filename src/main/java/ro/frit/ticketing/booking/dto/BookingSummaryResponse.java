package ro.frit.ticketing.booking.dto;

import lombok.Builder;
import lombok.experimental.FieldNameConstants;

@Builder
@FieldNameConstants
public record BookingSummaryResponse(String eventId,
                                     long bookings,
                                     long seats,
                                     Double averageSeatsPerBooking) {
}
