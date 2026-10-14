package ro.frit.ticketing.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder(toBuilder = true)
public record Venue(@NotBlank String name, @NotBlank String city) {
}