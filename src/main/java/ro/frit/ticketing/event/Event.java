package ro.frit.ticketing.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// Stored as one document in the events collection, with the venue embedded in it.
// The constraints are checked before every save, by the ValidatingEntityCallback in MongoConfig.
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
@Document(collection = "events")
public class Event {
    @Id
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
