package ro.frit.ticketing.event;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;
import ro.frit.ticketing.event.dto.EventSearchRequest;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventMapper eventMapper;
    private final EventRepository eventRepository;

    public List<EventResponse> search(EventSearchRequest searchRequest) {
        return toResponses(eventRepository.search(searchRequest));
    }

    public EventResponse findById(String id) {
        return eventMapper.toResponse(getEvent(id));
    }

    public boolean existsById(String id) {
        return eventRepository.existsById(id);
    }

    public EventResponse create(CreateEventRequest request) {
        Event event = eventMapper.toEntity(request);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    public List<EventResponse> findTopByBookedSeats(int limit) {
        return toResponses(eventRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Event::getBookedSeats).reversed()
                        .thenComparing(Event::getName))
                .limit(limit)
                .toList());
    }

    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 500)
    public void reserveSeats(String eventId, int seats) {
        Event event = getEvent(eventId);

        if (!event.getStartsAt().isAfter(LocalDateTime.now())) {
            throw new EventAlreadyStartedException(eventId, event.getStartsAt());
        }

        if (seats > event.availableSeats()) {
            throw new NotEnoughSeatsException(eventId, event.availableSeats());
        }

        eventRepository.save(event.toBuilder()
                .bookedSeats(event.getBookedSeats() + seats)
                .build());
    }

    private Event getEvent(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    private List<EventResponse> toResponses(List<Event> events) {
        return events.stream()
                .map(eventMapper::toResponse)
                .toList();
    }
}
