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

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {
    private static final String VALID_BODY = """
            {"customerEmail":"ana@mail.ro","seats":2}""";

    private static final BookingRequest VALID_REQUEST = new BookingRequest("e1", "ana@mail.ro", 2);

    private static final Booking BOOKING = Booking.builder()
            .id("b1")
            .eventId("e1")
            .customerEmail("ana@mail.ro")
            .seats(2)
            .bookedAt(LocalDateTime.of(2030, 1, 5, 10, 30))
            .build();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketOffice ticketOffice;

    @Test
    void confirmedBookingIsCreated() throws Exception {
        when(ticketOffice.book(VALID_REQUEST)).thenReturn(new BookingResult.Confirmed(BOOKING));

        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("b1"))
                .andExpect(jsonPath("$.eventId").value("e1"))
                .andExpect(jsonPath("$.customerEmail").value("ana@mail.ro"))
                .andExpect(jsonPath("$.seats").value(2))
                .andExpect(jsonPath("$.bookedAt").value("2030-01-05T10:30:00"));
    }

    @Test
    void eventIdComesFromThePathNotTheBody() throws Exception {
        when(ticketOffice.book(VALID_REQUEST)).thenReturn(new BookingResult.Confirmed(BOOKING));

        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON).content("""
                        {"eventId":"other","customerEmail":"ana@mail.ro","seats":2}"""))
                .andExpect(status().isCreated());

        verify(ticketOffice).book(VALID_REQUEST);
    }

    @Test
    void unknownEventIsNotFound() throws Exception {
        when(ticketOffice.book(new BookingRequest("e9", "ana@mail.ro", 2)))
                .thenReturn(new BookingResult.UnknownEvent("e9"));

        mockMvc.perform(post("/events/e9/bookings").contentType(APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("No event with id e9"));
    }

    @Test
    void soldOutIsConflictWithAvailableSeats() throws Exception {
        when(ticketOffice.book(VALID_REQUEST)).thenReturn(new BookingResult.SoldOut("e1", 1));

        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.availableSeats").value(1));
    }

    @Test
    void alreadyStartedIsConflictWithStartsAt() throws Exception {
        when(ticketOffice.book(VALID_REQUEST))
                .thenReturn(new BookingResult.AlreadyStarted("e1", LocalDateTime.of(2026, 3, 1, 20, 0)));

        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.startsAt").value("2026-03-01T20:00:00"));
    }

    @Test
    void zeroSeatsIsBadRequest() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON).content("""
                        {"customerEmail":"ana@mail.ro","seats":0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));

        verify(ticketOffice, never()).book(any());
    }

    @Test
    void negativeSeatsIsBadRequest() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON).content("""
                        {"customerEmail":"ana@mail.ro","seats":-1}"""))
                .andExpect(status().isBadRequest());

        verify(ticketOffice, never()).book(any());
    }

    @Test
    void blankEmailIsBadRequest() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON).content("""
                        {"customerEmail":"","seats":2}"""))
                .andExpect(status().isBadRequest());

        verify(ticketOffice, never()).book(any());
    }

    @Test
    void invalidEmailIsBadRequest() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON).content("""
                        {"customerEmail":"not-an-email","seats":2}"""))
                .andExpect(status().isBadRequest());

        verify(ticketOffice, never()).book(any());
    }

    @Test
    void missingEmailIsBadRequest() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON).content("""
                        {"seats":2}"""))
                .andExpect(status().isBadRequest());

        verify(ticketOffice, never()).book(any());
    }

    @Test
    void missingSeatsIsBadRequest() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON).content("""
                        {"customerEmail":"ana@mail.ro"}"""))
                .andExpect(status().isBadRequest());

        verify(ticketOffice, never()).book(any());
    }

    @Test
    void brokenJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON).content("""
                        {"customerEmail":"ana@mail.ro","seats":"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));

        verify(ticketOffice, never()).book(any());
    }

    @Test
    void wrongFieldTypeIsBadRequest() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(APPLICATION_JSON).content("""
                        {"customerEmail":"ana@mail.ro","seats":"two"}"""))
                .andExpect(status().isBadRequest());

        verify(ticketOffice, never()).book(any());
    }

    @Test
    void notJsonIsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(TEXT_PLAIN).content("ana@mail.ro, 2 seats"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));

        verify(ticketOffice, never()).book(any());
    }

    @Test
    void missingContentTypeIsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").content(VALID_BODY))
                .andExpect(status().isUnsupportedMediaType());

        verify(ticketOffice, never()).book(any());
    }
}
