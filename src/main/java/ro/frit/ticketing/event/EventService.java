package ro.frit.ticketing.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventMapper eventMapper;
    private final EventRepository eventRepository;

    public List<EventResponse> findAll() {
        return toResponses(eventRepository.findAll());
    }

    public EventResponse findById(String id) {
        return eventMapper.toResponse(getEvent(id));
    }

    public EventResponse create(CreateEventRequest request) {
        Event event = eventMapper.toEntity(UUID.randomUUID().toString(), request);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    public List<EventResponse> findWithFreeSeats() {
        return toResponses(eventRepository.findAll().stream()
                .filter(event -> event.availableSeats() > 0)
                .toList());
    }

    public List<EventResponse> findUpcomingIn(String city) {
        LocalDateTime now = LocalDateTime.now();
        return toResponses(eventRepository.findByVenueCityIgnoreCase(city).stream()
                .filter(event -> event.getStartsAt().isAfter(now))
                .sorted(Comparator.comparing(Event::getStartsAt))
                .toList());
    }

    public List<EventResponse> findTopByBookedSeats(int limit) {
        return toResponses(eventRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Event::getBookedSeats).reversed()
                        .thenComparing(Event::getName))
                .limit(limit)
                .toList());
    }

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
