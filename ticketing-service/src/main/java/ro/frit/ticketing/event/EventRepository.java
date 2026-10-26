package ro.frit.ticketing.event;

import org.springframework.data.mongodb.repository.MongoRepository;

// The search is a fragment: EventSearchRepository declares it, EventSearchRepositoryImpl builds it with MongoTemplate.
public interface EventRepository extends MongoRepository<Event, String>, EventSearchRepository {
}
