package ro.frit.ticketing.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller test ({@code @WebMvcTest}, the service is a mock): the HTTP side of the events API.
 */
@WebMvcTest(EventController.class)
class EventControllerTest {
    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2030, 6, 12, 20, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Test
    void listsAllEvents() throws Exception {
        when(eventService.findAll()).thenReturn(List.of(
                event("e1", "Concert", "Cluj", 100, 70),
                event("e2", "Play", "Iasi", 50, 50)));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("e1"))
                .andExpect(jsonPath("$[0].city").value("Cluj"))
                .andExpect(jsonPath("$[0].availableSeats").value(70));
    }

    @Test
    void returnsEventById() throws Exception {
        when(eventService.findById("e1")).thenReturn(event("e1", "Concert", "Cluj", 100, 70));

        mockMvc.perform(get("/events/e1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("e1"))
                .andExpect(jsonPath("$.availableSeats").value(70));
    }

    @Test
    void unknownIdIsNotFoundProblemDetail() throws Exception {
        when(eventService.findById("e9")).thenThrow(new EventNotFoundException("e9"));

        mockMvc.perform(get("/events/e9"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No event with id e9"));
    }

    @Test
    void cityListsUpcomingEventsInThatCity() throws Exception {
        when(eventService.findUpcomingIn("cluj")).thenReturn(List.of(event("e1", "Concert", "Cluj", 100, 100)));

        mockMvc.perform(get("/events").param("city", "cluj"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].city").value("Cluj"));

        verify(eventService, never()).findAll();
    }

    @Test
    void topListsTheMostBookedEvents() throws Exception {
        when(eventService.findTopByBookedSeats(2)).thenReturn(List.of(
                event("e1", "Concert", "Cluj", 100, 10),
                event("e2", "Play", "Iasi", 50, 40)));

        mockMvc.perform(get("/events/top").param("n", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("e1"))
                .andExpect(jsonPath("$[1].id").value("e2"));
    }

    @Test
    void topWithoutNListsThree() throws Exception {
        when(eventService.findTopByBookedSeats(3)).thenReturn(List.of());

        mockMvc.perform(get("/events/top"))
                .andExpect(status().isOk());

        verify(eventService).findTopByBookedSeats(3);
    }

    @Test
    void topWithZeroIsBadRequest() throws Exception {
        mockMvc.perform(get("/events/top").param("n", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));

        verify(eventService, never()).findTopByBookedSeats(anyInt());
    }

    @Test
    void newEventIsCreatedWithLocation() throws Exception {
        CreateEventRequest request = new CreateEventRequest("Rock Night", "BT Arena", "Cluj", STARTS_AT, 500);
        when(eventService.create(request)).thenReturn(event("e7", "Rock Night", "Cluj", 500, 500));

        mockMvc.perform(post("/events").contentType(APPLICATION_JSON).content("""
                        {"name":"Rock Night","venueName":"BT Arena","city":"Cluj",
                         "startsAt":"2030-06-12T20:00:00","capacity":500}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/events/e7")))
                .andExpect(jsonPath("$.name").value("Rock Night"))
                .andExpect(jsonPath("$.availableSeats").value(500));
    }

    @Test
    void eventInThePastIsBadRequest() throws Exception {
        mockMvc.perform(post("/events").contentType(APPLICATION_JSON).content("""
                        {"name":"Rock Night","venueName":"BT Arena","city":"Cluj",
                         "startsAt":"2020-06-12T20:00:00","capacity":500}"""))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).create(any());
    }

    @Test
    void eventWithoutNameIsBadRequest() throws Exception {
        mockMvc.perform(post("/events").contentType(APPLICATION_JSON).content("""
                        {"venueName":"BT Arena","city":"Cluj","startsAt":"2030-06-12T20:00:00","capacity":500}"""))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).create(any());
    }

    private static EventResponse event(String id, String name, String city, int capacity, int availableSeats) {
        return new EventResponse(id, name, "Arena", city, STARTS_AT, capacity, availableSeats);
    }
}
