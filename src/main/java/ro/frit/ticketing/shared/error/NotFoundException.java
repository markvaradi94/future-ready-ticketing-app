package ro.frit.ticketing.shared.error;

/**
 * Base class for "the thing you asked for does not exist". {@link GlobalExceptionHandler} answers it with 404.
 */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
