package ro.frit.ticketing.event;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import ro.frit.ticketing.event.dto.EventSearchRequest;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static ro.frit.ticketing.event.Event.Fields.name;
import static ro.frit.ticketing.event.Event.Fields.startsAt;
import static ro.frit.ticketing.event.Event.Fields.venue;

@RequiredArgsConstructor
public class EventSearchRepositoryImpl implements EventSearchRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public List<Event> search(EventSearchRequest filter) {
        // One Optional per filter: empty when the parameter was not sent, so flatMap drops it.
        // Pattern.quote makes the user's text plain text, not a regex; ^ and $ make the city match as a whole.
        List<Criteria> criteria = Stream.of(
                        ofNullable(filter.city())
                                .map(city -> Criteria.where(venue + "." + Venue.Fields.city)
                                        .regex("^" + Pattern.quote(city) + "$", "i")),
                        ofNullable(filter.name())
                                .map(text -> Criteria.where(name).regex(Pattern.quote(text), "i")),
                        ofNullable(filter.from())
                                .map(from -> Criteria.where(startsAt).gte(from)),
                        ofNullable(filter.to())
                                .map(to -> Criteria.where(startsAt).lt(to)))
                .flatMap(Optional::stream)
                .toList();

        // No filter matches every event. MongoDB rejects an $and with an empty list.
        Query query = criteria.isEmpty() ? new Query() : new Query(new Criteria().andOperator(criteria));
        return mongoTemplate.find(query.with(Sort.by(startsAt)), Event.class);
    }
}
