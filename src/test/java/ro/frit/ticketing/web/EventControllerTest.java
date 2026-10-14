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
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}