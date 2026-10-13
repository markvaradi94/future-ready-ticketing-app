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
- `ro.frit.features` holds the tour of Java releases, one class for each: `Java8Features`, `Java11Features`, `Java17Features`, `Java21Features`, `Java25Features`. Each has a `main` method you can run.

The first commit is the code exactly as we wrote it in class. The second adds what we did not reach: the rest of `TicketOffice` with its tests, and the tour classes for Java 11, 21 and 25.

## Running it

You need JDK 25.

```
./gradlew test
```
