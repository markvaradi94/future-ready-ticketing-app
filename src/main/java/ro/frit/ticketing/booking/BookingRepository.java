package ro.frit.ticketing.booking;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BookingRepository extends MongoRepository<Booking, String> {
}
