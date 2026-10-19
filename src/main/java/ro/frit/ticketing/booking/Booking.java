package ro.frit.ticketing.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.persistence.*;
import lombok.*;
import ro.frit.ticketing.event.Event;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @NotBlank
    private String id;

    // The service writes the event id; the event_id column is a foreign key to events.
    @NotBlank
    @Column(name = "event_id")
    private String eventId;

    // The same column, read as the Event it points to. Loaded lazily: only when someone calls getEvent().
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Event event;

    @NotBlank
    @Email
    private String customerEmail;

    @Positive
    private int seats;

    @NotNull
    private LocalDateTime bookedAt;
}
