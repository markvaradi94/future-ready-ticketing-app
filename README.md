# Ticketing App

The project we build together in the FutureReady IT course "Modern Java Architecture & Cloud Deployment": a ticketing system that grows, session by session, from plain Java into Spring Boot services with MongoDB, RabbitMQ, an API gateway, Docker and a cloud deployment.

## How this repository is organised

There is one project on `main`, and one tag for each session: `session-01`, `session-02`, and so on. A tag is the state of the code at the end of that session.

To get the code of a session:

```
git clone https://github.com/markvaradi94/future-ready-ticketing-app.git
cd future-ready-ticketing-app
git checkout session-01
```

You can also pick the tag in the branch menu on GitHub and use **Code > Download ZIP**.

## Session 1: Modern Java

- `ro.frit.ticketing.domain` holds the domain: `Event`, `Venue`, `Booking`, `BookingRequest`, the sealed `BookingResult` and `TicketOffice` with the booking rules.
- `ro.frit.features` holds the tour of Java releases, one class for each: `Java8Features`, `Java11Features`, `Java17Features`, `Java21Features`, `Java25Features`. Each has a `main` method you can run. From Session 2 on, they are only in the `session-01` tag.
- `Hello.java`, next to this file, is a Java 25 compact source file. Run it with `java Hello.java`.

The first commit is the code exactly as we wrote it in class. The commits after it add what we did not reach:

- the rest of `TicketOffice` with its tests, and the tour classes for Java 11, 21 and 25;
- the parts of the Java 8 and Java 17 tour we skipped (each method is marked 'Added after the session'), and three more `TicketOffice` methods that use newer Java: `bookAll`, `lastBooking` and `bookingsInBatches`.

## Session 2: Spring Framework and Spring Boot

In Session 2 we removed the Java tour classes from `ro.frit.features`: each had its own `main` method, and a Spring Boot application needs exactly one. They are still in the `session-01` tag.

- `TicketingApplication` starts the Spring Boot application.
- `ro.frit.ticketing.config` creates the `TicketOffice` bean, filled with the events from the settings (`TicketingProperties`). The `local` profile, in `application-local.yml`, adds three events.
- `ro.frit.ticketing.service` holds `TicketOffice`, which moved out of `domain`.
- `ro.frit.ticketing.web` holds the REST API:
  - `EventController`: `GET /events`, `GET /events/{id}`
  - `BookingController`: `POST /events/{eventId}/bookings`, which turns each `BookingResult` into its own status code
  - errors go out as a `ProblemDetail`
- The tests for the web layer are `@WebMvcTest` tests: `EventControllerTest` and `BookingControllerTest`.

### Tests first, code by AI

We wrote the booking endpoint in this order:

1. The behaviour, as a table of cases: `docs/booking-cases.md`.
2. A test for each case, written by hand, in `BookingControllerTest`.
3. Run the tests and see them fail.
4. Commit the tests.
5. Claude Code wrote `BookingController` to make them pass.
6. We reviewed it: the tests pass, no test changed, and we read the code.

`CLAUDE.md` holds the rules Claude Code reads at the start. `.claude/settings.json` stops it from editing anything under `src/test`. `.github/workflows/build.yml` runs `./gradlew build` on GitHub on every push.

### Added after the session

The commits after the class code:
- tidy it up;
- turn the list of cases into a table that names the test for each case;
- add the Claude Code permission and the HTTP requests in `http/`;
- add the four stretch goals, each marked 'Added after the session':
  - `GET /events?city=cluj`
  - `GET /events/top?n=3`
  - `POST /events`
  - `GET /events/{eventId}/bookings/{bookingId}`

## Before Session 3: the code organised like an enterprise service

Between Sessions 2 and 3 the code was reorganised the way most Spring teams organise a service: by feature, with one controller, service and repository for each. It does exactly what it did at the end of Session 2, and the HTTP API is unchanged.

```
ro.frit.ticketing
├── event      Event, Venue, EventController, EventService, EventRepository, EventMapper,
│              EventNotFoundException, NotEnoughSeatsException, EventAlreadyStartedException,
│              dto/ CreateEventRequest, EventResponse
├── booking    Booking, BookingController, BookingService, BookingRepository, BookingMapper,
│              BookingNotFoundException, dto/ CreateBookingRequest, BookingResponse
├── shared     error/ NotFoundException, ConflictException, GlobalExceptionHandler
└── config     TicketingProperties, DataSeeder
```

What changed, and why:

- **`TicketOffice` is split** into `EventService` and `BookingService`. `EventService` owns the event rules: `reserveSeats` checks that the event has not started and has enough seats. `BookingService` asks it to reserve the seats, then stores the booking.
- **Repositories.** `EventRepository` and `BookingRepository` keep the data in memory, with the method names Spring Data uses (`save`, `findById`, `findAll`). In Session 3 they become MongoDB repositories.
- **Errors are exceptions.** The sealed `BookingResult` is gone from the application (it is still in the `session-02` tag). A service throws, for example, `NotEnoughSeatsException`; `GlobalExceptionHandler` turns every `NotFoundException` into a 404 and every `ConflictException` into a 409, as a `ProblemDetail`. The controllers only describe the happy path.
- **Validation with annotations only.** `Event`, `Venue` and `Booking` declare their rules with Jakarta Bean Validation (`@NotBlank`, `@Positive`, `@Email`, ...) instead of `if` checks in the constructor. The request DTOs are checked when a request arrives (`@Valid` in the controller, 400 if invalid); the entities are checked when they are saved (`@Validated` repositories with a `@Valid` parameter on `save`).
- **DTOs and mappers.** Each feature has its request and response records in `dto`, and a mapper (`EventMapper`, `BookingMapper`) that converts between them and the entity. The services return DTOs, so entities never leave the service.
- **`DataSeeder`** replaces `TicketingConfig`: under the `local` profile it saves the seed events when the application starts.
- **Tests**: `EventServiceTest` and `BookingServiceTest` replace `TicketOfficeTest`; the controller tests mock the services. The Java tour methods `bookAll`, `lastBooking` and `bookingsInBatches` were removed; they are in the `session-01` and `session-02` tags.

## Running it

You need JDK 25.

```
./gradlew test
./gradlew bootRun --args='--spring.profiles.active=local'
```

The application then listens on `http://localhost:8085`. Open `http/events.http` and `http/bookings.http` in IntelliJ to send the requests.
