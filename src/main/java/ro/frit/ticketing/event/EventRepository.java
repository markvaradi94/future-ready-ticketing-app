package ro.frit.ticketing.event;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {

    // Spring Data writes the query from the method name: venue.city equal ignoring case, startsAt after the given
    // time, sorted by startsAt.
    List<Event> findByVenueCityIgnoreCaseAndStartsAtAfterOrderByStartsAt(String city, LocalDateTime after);
}
