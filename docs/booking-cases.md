# Booking seats: POST /events/{eventId}/bookings

Body: `{"customerEmail": "ana@mail.ro", "seats": 2}`

Every row is checked by the tests in `BookingControllerTest`.

| Case | Status | The response also has | Test |
| --- | --- | --- | --- |
| The service confirms the booking | 201 | the booking: `id`, `eventId`, `customerEmail`, `seats`, `bookedAt` | `confirmedBookingIsCreated` |
| The event id in the body is ignored; the path decides | 201 | | `eventIdComesFromThePathNotTheBody` |
| No such event (`EventNotFoundException`) | 404 | `detail`: "No event with id e9" | `unknownEventIsNotFound` |
| Not enough seats (`NotEnoughSeatsException`) | 409 | `availableSeats` | `soldOutIsConflictWithAvailableSeats` |
| The event has already started (`EventAlreadyStartedException`) | 409 | `startsAt` | `alreadyStartedIsConflictWithStartsAt` |
| Invalid email: blank, or not an email | 400 | the service is never called | `blankEmailIsBadRequest`, `invalidEmailIsBadRequest` |
| Invalid number of seats: zero or negative | 400 | the service is never called | `zeroSeatsIsBadRequest`, `negativeSeatsIsBadRequest` |
| Incomplete body: no email, or no seats | 400 | the service is never called | `missingEmailIsBadRequest`, `missingSeatsIsBadRequest` |
| Broken JSON | 400 | the service is never called | `brokenJsonIsBadRequest` |
| A field of the wrong type | 400 | the service is never called | `wrongFieldTypeIsBadRequest` |
| Not JSON, or no content type | 415 | the service is never called | `notJsonIsUnsupportedMediaType`, `missingContentTypeIsUnsupportedMediaType` |

All errors are a `ProblemDetail`, with the content type `application/problem+json`. The 404 and 409 answers come from `GlobalExceptionHandler`; the 400 and 415 answers come from Spring Boot's own handler.

The rules behind the 404 and 409 (exactly the free seats, an event that starts right now, started is checked before seats) are checked in `EventServiceTest` and `BookingServiceTest`, because the controller tests replace the service with a mock.

## Not yet decided

- Can the same customer book the same event twice? Today, yes.
- Should the 201 have a `Location` header? `GET /events/{eventId}/bookings/{bookingId}` now exists, so it could.
