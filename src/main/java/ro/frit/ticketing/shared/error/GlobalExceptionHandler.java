package ro.frit.ticketing.shared.error;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Turns the application's exceptions into {@link ProblemDetail} responses, in one place, so the controllers only
 * describe the happy path. Spring Boot's own handler (enabled by {@code spring.mvc.problemdetails.enabled}) still
 * answers the framework's errors: invalid bodies, wrong content types, failed validation.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(CONFLICT, exception.getMessage());
        exception.properties().forEach(problem::setProperty);
        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException exception) {
        return ProblemDetail.forStatusAndDetail(CONFLICT, "Someone else updated the object while you were editing it.");
    }
}