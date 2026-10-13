package ro.frit.ticketing.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Booking {
    private String id;
    private String eventId;
    private String customerEmail;
    private int seats;
    private LocalDateTime bookedAt;
}
