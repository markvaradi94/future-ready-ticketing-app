package ro.frit.ticketing.booking;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import ro.frit.ticketing.booking.dto.BookingSummaryResponse;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static ro.frit.ticketing.booking.Booking.Fields.eventId;
import static ro.frit.ticketing.booking.Booking.Fields.seats;
import static ro.frit.ticketing.booking.BookingReportRepositoryImpl.SeatsPerEvent.Fields.totalSeats;
import static ro.frit.ticketing.booking.dto.BookingSummaryResponse.Fields.bookings;

@RequiredArgsConstructor
public class BookingReportRepositoryImpl implements BookingReportRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public Map<String, Integer> sumSeatsPerEvent() {
        Aggregation pipeline = Aggregation.newAggregation(
                Aggregation.group(eventId).sum(seats).as(totalSeats));

        return mongoTemplate.aggregate(pipeline, Booking.class, SeatsPerEvent.class)
                .getMappedResults().stream()
                .collect(Collectors.toMap(SeatsPerEvent::id, SeatsPerEvent::totalSeats));
    }

    @Override
    public Optional<BookingSummaryResponse> summaryFor(String eventId) {
        Aggregation pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where(Booking.Fields.eventId).is(eventId)),
                Aggregation.group(Booking.Fields.eventId)
                        .count().as(bookings)
                        .sum(seats).as(BookingSummaryResponse.Fields.seats)
                        .avg(seats).as(BookingSummaryResponse.Fields.averageSeatsPerBooking),
                // Renames the group's _id to eventId, so the result maps straight onto the response.
                Aggregation.project(bookings, BookingSummaryResponse.Fields.seats,
                                BookingSummaryResponse.Fields.averageSeatsPerBooking)
                        .and(BookingSummaryResponse.Fields.eventId).previousOperation());

        return Optional.ofNullable(mongoTemplate.aggregate(pipeline, Booking.class, BookingSummaryResponse.class)
                .getUniqueMappedResult());
    }

    @FieldNameConstants
    record SeatsPerEvent(String id, int totalSeats) {
    }
}
