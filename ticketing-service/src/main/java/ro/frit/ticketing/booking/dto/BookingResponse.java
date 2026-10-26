package ro.frit.ticketing.booking.dto;

import java.time.LocalDateTime;

public record BookingResponse(String id,
                              String eventId,
                              String customerEmail,
                              int seats,
                              LocalDateTime bookedAt) {
}
