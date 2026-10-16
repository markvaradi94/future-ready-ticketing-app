package ro.frit.ticketing.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;

// The constraints are checked when the booking is saved: BookingRepository.save takes a @Valid Booking.
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Booking {
    @NotBlank
    private String id;

    @NotBlank
    private String eventId;

    @NotBlank
    @Email
    private String customerEmail;

    @Positive
    private int seats;

    @NotNull
    private LocalDateTime bookedAt;
}
