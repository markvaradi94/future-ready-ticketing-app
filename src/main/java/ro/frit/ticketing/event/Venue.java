package ro.frit.ticketing.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

// Embedded in the events table: no table of its own, two columns in the event's row.
@Builder(toBuilder = true)
@Embeddable
public record Venue(@NotBlank @Column(name = "venue_name") String name,
                    @NotBlank @Column(name = "venue_city") String city) {
}
