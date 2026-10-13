package ro.frit.ticketing.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Event {
    private String id;
    private String name;
    private Venue venue;
    private LocalDateTime startsAt;
    private int capacity;
    private int bookedSeats;

    public int availableSeats() {
        return capacity - bookedSeats;
    }
}