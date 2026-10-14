# Booking seats: POST /events/{eventId}/bookings

Body: `{"customerEmail": "ana@mail.ro", "seats": 2}`

Every row is checked by the tests in `BookingControllerTest`.

| Case | Status | The response also has | Test |
| --- | --- | --- | --- |
| The office confirms the booking | 201 | the booking: `id`, `eventId`, `customerEmail`, `seats`, `bookedAt` | `confirmedBookingIsCreated` |
| The event id in the body is ignored; the path decides | 201 | | `eventIdComesFromThePathNotTheBody` |
| No such event | 404 | `detail`: "No event with id e9" | `unknownEventIsNotFound` |
| Not enough seats | 409 | `availableSeats` | `soldOutIsConflictWithAvailableSeats` |
| The event has already started | 409 | `startsAt` | `alreadyStartedIsConflictWithStartsAt` |
| Invalid email: blank, or not an email | 400 | the office is never called | `blankEmailIsBadRequest`, `invalidEmailIsBadRequest` |
| Invalid number of seats: zero or negative | 400 | the office is never called | `zeroSeatsIsBadRequest`, `negativeSeatsIsBadRequest` |
| Incomplete body: no email, or no seats | 400 | the office is never called | `missingEmailIsBadRequest`, `missingSeatsIsBadRequest` |
| Broken JSON | 400 | the office is never called | `brokenJsonIsBadRequest` |
| A field of the wrong type | 400 | the office is never called | `wrongFieldTypeIsBadRequest` |
| Not JSON, or no content type | 415 | the office is never called | `notJsonIsUnsupportedMediaType`, `missingContentTypeIsUnsupportedMediaType` |

All errors are a `ProblemDetail`, with the content type `application/problem+json`.

The rules inside the office (exactly the free seats, an event that starts right now) are checked in `TicketOfficeTest`, because the controller tests replace the office with a mock.

## Not yet decided

- Can the same customer book the same event twice? Today, yes.
- Should the 201 have a `Location` header? It would need an endpoint that reads one booking.
