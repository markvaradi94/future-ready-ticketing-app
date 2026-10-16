# Ticketing app

A Spring Boot 4 application on Java 25, built with Gradle. Run the tests with `./gradlew test`.

## How we work

- We write the tests by hand. They say what the system must do.
- You write the code that makes them pass.
- When you are done, run `./gradlew test` and report the result.

## Code rules

- The base package is `ro.frit.ticketing`, organised by feature: `event` and `booking` each hold their entity, controller, service, repository, mapper, exceptions and `dto` package. `shared.error` has the base exceptions and `GlobalExceptionHandler`. `config` has the Spring configuration.
- Controllers only call their service. Services return DTOs, built by the feature's mapper. `BookingService` goes through `EventService`, never through `EventRepository`.
- Entities (`Event`, `Booking`, `Venue`) import nothing from Spring.
- Validation uses Jakarta Bean Validation annotations (`@NotBlank`, `@Positive`, ...), on the DTOs and on the entities. No hand-written `if ... throw` checks for what an annotation can say. Entities are checked when they are saved: the repositories are `@Validated` and `save` takes a `@Valid` parameter.
- Business errors are exceptions that extend `NotFoundException` or `ConflictException`; `GlobalExceptionHandler` turns them into a `ProblemDetail`. No `try`/`catch` in controllers.
- Constructor injection with Lombok's `@RequiredArgsConstructor`. No field injection.
- Errors go out as `ProblemDetail`.
- Write the least code that makes the tests pass. No extra endpoints, no extra classes.
