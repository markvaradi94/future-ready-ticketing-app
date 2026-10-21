package ro.frit.ticketing.event;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;

@Builder(toBuilder = true)
@FieldNameConstants
public record Venue(@NotBlank String name,
                    @NotBlank String city) {
}
