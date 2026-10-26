package ro.frit.ticketing.shared.error;

import java.util.Map;

/**
 * Base class for "the request is valid, but the current state says no". {@link GlobalExceptionHandler} answers it
 * with 409, and adds the {@link #properties()} to the problem detail.
 */
public abstract class ConflictException extends RuntimeException {

    private final Map<String, Object> properties;

    protected ConflictException(String message, Map<String, Object> properties) {
        super(message);
        this.properties = Map.copyOf(properties);
    }

    public Map<String, Object> properties() {
        return properties;
    }
}
