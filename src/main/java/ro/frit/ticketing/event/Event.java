package ro.frit.ticketing.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

// One row in the events table; the venue's two fields are columns of the same row.
// Hibernate checks the constraints before every insert and update.
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "events")
public class Event {
    @Id
    @NotBlank
    private String id;

    @NotBlank
    private String name;

    @NotNull
    @Valid
    @Embedded
    private Venue venue;

    @NotNull
    private LocalDateTime startsAt;

    @Positive
    private int capacity;

    @PositiveOrZero
    private int bookedSeats;

    public int availableSeats() {
        return capacity - bookedSeats;
    }
}
