package ro.frit.ticketing.event.dto;

import java.time.LocalDateTime;

public record EventResponse(String id,
                            String name,
                            String venueName,
                            String city,
                            LocalDateTime startsAt,
                            int capacity,
                            int availableSeats) {
}
