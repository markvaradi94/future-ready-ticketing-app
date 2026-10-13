package ro.frit.ticketing.domain;

import lombok.Builder;

@Builder(toBuilder = true)
public record Venue(String name, String city) {

    public Venue {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }

        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be blank");
        }
    }
}