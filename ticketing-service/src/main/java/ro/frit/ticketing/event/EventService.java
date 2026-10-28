package ro.frit.ticketing.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;
import ro.frit.ticketing.event.dto.EventSearchRequest;
import ro.frit.ticketing.event.dto.UpdateEventRequest;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventMapper eventMapper;
    private final EventRepository eventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

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

    public EventResponse update(String id, UpdateEventRequest request) {
        Event event = getEvent(id);

        if (request.capacity() < event.getBookedSeats()) {
            throw new CapacityBelowSoldSeatsException(id, event.getBookedSeats());
        }

        Event updatedEntity = eventRepository.save(eventMapper.update(event, request));
        // Added after the session (lab round 3): customers care about the name, the place and the time.
        if (!updatedEntity.getName().equals(event.getName()) || !updatedEntity.getVenue().equals(event.getVenue())
                || !updatedEntity.getStartsAt().equals(event.getStartsAt())) {
            applicationEventPublisher.publishEvent(EventChanged.of(updatedEntity));
        }
        return eventMapper.toResponse(updatedEntity);
    }

    public void delete(String eventId) {
        Event event = getEvent(eventId);
        eventRepository.delete(event);
        applicationEventPublisher.publishEvent(EventCancelled.of(event));
    }

    public List<EventResponse> findTopByBookedSeats(int limit) {
        return toResponses(eventRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Event::getBookedSeats).reversed()
                        .thenComparing(Event::getName))
                .limit(limit)
                .toList());
    }

    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 500)
    public EventResponse reserveSeats(String eventId, int seats) {
        Event event = getEvent(eventId);

        if (!event.getStartsAt().isAfter(LocalDateTime.now())) {
            throw new EventAlreadyStartedException(eventId, event.getStartsAt());
        }

        if (seats > event.availableSeats()) {
            throw new NotEnoughSeatsException(eventId, event.availableSeats());
        }

        return eventMapper.toResponse(eventRepository.save(event.toBuilder()
                .bookedSeats(event.getBookedSeats() + seats)
                .build()));
    }

    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 500)
    public void releaseSeats(String eventId, int seats) {
        Event event = getEvent(eventId);

        if (!event.getStartsAt().isAfter(LocalDateTime.now())) {
            throw new EventAlreadyStartedException(eventId, event.getStartsAt());
        }

        eventRepository.save(event.toBuilder()
                .bookedSeats(event.getBookedSeats() - seats)
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
