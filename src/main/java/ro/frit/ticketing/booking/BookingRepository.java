package ro.frit.ticketing.booking;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends MongoRepository<Booking, String>, BookingReportRepository {
    Optional<Booking> findByIdAndEventId(String id, String eventId);

    List<Booking> findByEventIdOrderByBookedAt(String eventId);
}