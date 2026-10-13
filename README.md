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
- `Hello.java`, next to this file, is a Java 25 compact source file. Run it with `java Hello.java`.

The first commit is the code exactly as we wrote it in class. The commits after it add what we did not reach:

- the rest of `TicketOffice` with its tests, and the tour classes for Java 11, 21 and 25;
- the parts of the Java 8 and Java 17 tour we skipped (each method is marked 'Added after the session'), and three more `TicketOffice` methods that use newer Java: `bookAll`, `lastBooking` and `bookingsInBatches`.

## Running it

You need JDK 25.

```
./gradlew test
```
