package ro.frit.ticketing.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
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
@FieldNameConstants
@Document(collection = "events")
public class Event {
    @Id
    private String id;

    // Optimistic locking: a save only succeeds if the stored version is still the one that was read. Spring Data
    // increases it on every save, and throws OptimisticLockingFailureException when someone else saved first.
    @Version
    private long version;

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
