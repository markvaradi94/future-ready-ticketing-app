package ro.frit.ticketing.web;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.frit.ticketing.domain.Event;
import ro.frit.ticketing.domain.Venue;
import ro.frit.ticketing.service.TicketOffice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketOffice ticketOffice;

    private static Event event(String id, String name, String city, int capacity, int bookedSeats) {
        return Event.builder()
                .id(id)
                .name(name)
                .venue(new Venue("Arena", city))
                .startsAt(LocalDateTime.of(2030, 6, 12, 20, 0))
                .capacity(capacity)
                .bookedSeats(bookedSeats)
                .build();
    }

    @Test
    void listAllEvents() throws Exception {
        when(ticketOffice.allEvents()).thenReturn(
                List.of(event("e1", "Concert", "Cluj", 100, 30),
                        event("e2", "Play", "Iasi", 50, 0)));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("e1"))
                .andExpect(jsonPath("$[0].city").value("Cluj"))
                .andExpect(jsonPath("$[0].availableSeats").value(70));
    }

    @Test
    void returnsEventById() throws Exception {
        when(ticketOffice.findEvent("e1")).thenReturn(Optional.of(event("e1", "Concert", "Cluj", 100, 30)));

        mockMvc.perform(get("/events/e1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("e1"))
                .andExpect(jsonPath("$.availableSeats").value(70));
    }

    @Test
    void unknownIdIsNotFoundProblemDetail() throws Exception {
        when(ticketOffice.findEvent("e9")).thenReturn(Optional.empty());

        mockMvc.perform(get("/events/e9"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(containsString("e9")));
    }

    // Added after the session (stretch 1): GET /events?city=...

    @Test
    void cityListsUpcomingEventsInThatCity() throws Exception {
        when(ticketOffice.upcomingEventsIn(eq("cluj"), any(LocalDateTime.class)))
                .thenReturn(List.of(event("e1", "Concert", "Cluj", 100, 0)));

        mockMvc.perform(get("/events").param("city", "cluj"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].city").value("Cluj"));

        verify(ticketOffice, never()).allEvents();
    }

    // Added after the session (stretch 3): GET /events/top?n=...

    @Test
    void topListsTheMostBookedEvents() throws Exception {
        when(ticketOffice.topEventsByBookedSeats(2)).thenReturn(
                List.of(event("e1", "Concert", "Cluj", 100, 90), event("e2", "Play", "Iasi", 50, 10)));

        mockMvc.perform(get("/events/top").param("n", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("e1"))
                .andExpect(jsonPath("$[1].id").value("e2"));
    }

    @Test
    void topWithoutNListsThree() throws Exception {
        when(ticketOffice.topEventsByBookedSeats(3)).thenReturn(List.of());

        mockMvc.perform(get("/events/top"))
                .andExpect(status().isOk());

        verify(ticketOffice).topEventsByBookedSeats(3);
    }

    @Test
    void topWithZeroIsBadRequest() throws Exception {
        mockMvc.perform(get("/events/top").param("n", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));

        verify(ticketOffice, never()).topEventsByBookedSeats(anyInt());
    }

    // Added after the session (stretch 4): POST /events.

    @Test
    void newEventIsCreatedWithLocation() throws Exception {
        when(ticketOffice.addEvent(any(Event.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(post("/events").contentType(APPLICATION_JSON).content("""
                        {"name":"Rock Night","venueName":"BT Arena","city":"Cluj",
                         "startsAt":"2030-06-12T20:00:00","capacity":500}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/events/")))
                .andExpect(jsonPath("$.name").value("Rock Night"))
                .andExpect(jsonPath("$.availableSeats").value(500));
    }

    @Test
    void eventInThePastIsBadRequest() throws Exception {
        mockMvc.perform(post("/events").contentType(APPLICATION_JSON).content("""
                        {"name":"Rock Night","venueName":"BT Arena","city":"Cluj",
                         "startsAt":"2020-06-12T20:00:00","capacity":500}"""))
                .andExpect(status().isBadRequest());

        verify(ticketOffice, never()).addEvent(any());
    }

    @Test
    void eventWithoutNameIsBadRequest() throws Exception {
        mockMvc.perform(post("/events").contentType(APPLICATION_JSON).content("""
                        {"venueName":"BT Arena","city":"Cluj","startsAt":"2030-06-12T20:00:00","capacity":500}"""))
                .andExpect(status().isBadRequest());

        verify(ticketOffice, never()).addEvent(any());
    }
}