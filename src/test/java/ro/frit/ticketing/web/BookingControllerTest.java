package ro.frit.ticketing.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.frit.ticketing.domain.Booking;
import ro.frit.ticketing.domain.BookingRequest;
import ro.frit.ticketing.domain.BookingResult;
import ro.frit.ticketing.service.TicketOffice;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.MediaType.APPLICATION_JSON;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {
    private static final String VALID_BODY = """
            {"customerEmail":"ana@mail.ro","seats":2}""";

    private static final BookingRequest VALID_REQUEST = new BookingRequest("e1", "ana@mail.ro", 2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketOffice ticketOffice;

    @Test
    void confirmedBookingIsCreated() throws Exception {
        Booking booking = Booking.builder()
                .id("b1")
                .eventId("e1")
                .customerEmail("ana@mail.ro")
                .seats(2)
                .bookedAt(LocalDateTime.of(2030, 1, 5, 10, 30))
                .build();

        when(ticketOffice.book(VALID_REQUEST)).thenReturn(new BookingResult.Confirmed(booking));

        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON.toString()).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("b1"))
                .andExpect(jsonPath("$.eventId").value("e1"))
                .andExpect(jsonPath("$.seats").value(2));
    }

    @Test
    void unknownEventIsNotFound() throws Exception {
        when(ticketOffice.book(new BookingRequest("e9", "ana@mail.ro", 2)))
                .thenReturn(new BookingResult.UnknownEvent("e9"));

        mockMvc.perform(post("/events/e9/bookings").contentType(APPLICATION_JSON.toString()).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No event with id e9"));
    }

    @Test
    void soldOutIsConflictWithAvailableSeats() throws Exception {
        when(ticketOffice.book(VALID_REQUEST)).thenReturn(new BookingResult.SoldOut("e1", 1));

        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON.toString()).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.availableSeats").value(1));
    }

    @Test
    void alreadyStartedIsConflictWithStartsAt() throws Exception {
        when(ticketOffice.book(VALID_REQUEST))
                .thenReturn(new BookingResult.AlreadyStarted("e1", LocalDateTime.of(2026, 3, 1, 20, 0)));

        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON.toString()).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.startsAt").value("2026-03-01T20:00:00"));
    }

    @Test
    void zeroSeatsIsBadRequest() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON.toString()).content("""
                        {"customerEmail":"ana@mail.ro","seats":0}"""))
                .andExpect(status().isBadRequest());

        verify(ticketOffice, never()).book(any());
    }

    @Test
    void blankEmailIsBadRequest() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON.toString()).content("""
                        {"customerEmail":"","seats":2}"""))
                .andExpect(status().isBadRequest());

        verify(ticketOffice, never()).book(any());
    }
}