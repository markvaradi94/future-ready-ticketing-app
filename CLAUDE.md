# Ticketing app

A Spring Boot 4 application on Java 25, built with Gradle. Run the tests with `./gradlew test`.

## How we work

- We write the tests by hand. They say what the system must do.
- You write the code that makes them pass.
- When you are done, run `./gradlew test` and report the result.

## Code rules

- The base package is `ro.frit.ticketing`. `domain` has the business rules, `web` has the controllers, `config` has the Spring configuration.
- Nothing in `domain` imports Spring.
- Constructor injection with Lombok's `@RequiredArgsConstructor`. No field injection.
- Errors go out as `ProblemDetail`.
- A `switch` over `BookingResult` has no `default` branch.
- Write the least code that makes the tests pass. No extra endpoints, no extra classes.