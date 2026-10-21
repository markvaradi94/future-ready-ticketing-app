package ro.frit.ticketing.event;

import ro.frit.ticketing.event.dto.EventSearchRequest;

import java.util.List;

public interface EventSearchRepository {

    List<Event> search(EventSearchRequest request);
}
