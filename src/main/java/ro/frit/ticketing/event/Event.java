package ro.frit.ticketing.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;

// The constraints are checked when the event is saved: EventRepository.save takes a @Valid Event.
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Event {
    @NotBlank
    private String id;

    @NotBlank
    private String name;

    @NotNull
    @Valid
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
