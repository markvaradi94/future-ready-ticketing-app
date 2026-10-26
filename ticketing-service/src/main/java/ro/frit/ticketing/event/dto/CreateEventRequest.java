package ro.frit.ticketing.event.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record CreateEventRequest(@NotBlank String name,
                                 @NotBlank String venueName,
                                 @NotBlank String city,
                                 @NotNull @Future LocalDateTime startsAt,
                                 @Positive int capacity) {
}
