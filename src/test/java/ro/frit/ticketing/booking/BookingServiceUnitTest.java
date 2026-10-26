package ro.frit.ticketing.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.EventCancelled;
import ro.frit.ticketing.event.EventChanged;
import ro.frit.ticketing.event.EventService;
import ro.frit.ticketing.event.NotEnoughSeatsException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ro.frit.ticketing.booking.BookingCancelled.Reason.EVENT_CANCELLED;

/**
 * Unit test, no Spring and no Docker: a confirmed booking is announced exactly once, with what was stored, and a
 * refused one never.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceUnitTest {
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventService eventService;

    @Mock
    private BookingEventPublisher bookingEventPublisher;

    @Spy
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void confirmedBookingIsAnnouncedOnceWithWhatWasStored() {
        // The repository gives the booking its id, as MongoDB would.
        when(bookingRepository.save(any())).thenAnswer(call -> call.<Booking>getArgument(0).toBuilder().id("b1").build());
        ArgumentCaptor<BookingConfirmed> published = ArgumentCaptor.forClass(BookingConfirmed.class);

        bookingService.book("e1", new CreateBookingRequest("ana@mail.ro", 2));

        verify(bookingEventPublisher).publish(published.capture());
        assertEquals("b1", published.getValue().bookingId());
        assertEquals("e1", published.getValue().eventId());
        assertEquals("ana@mail.ro", published.getValue().customerEmail());
        assertEquals(2, published.getValue().seats());
    }

    @Test
    void soldOutBookingIsNotAnnounced() {
        doThrow(new NotEnoughSeatsException("e1", 1)).when(eventService).reserveSeats("e1", 2);

        assertThrows(NotEnoughSeatsException.class,
                () -> bookingService.book("e1", new CreateBookingRequest("ana@mail.ro", 2)));

        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(bookingEventPublisher);
    }

    @Test
    void cancelledEventCancelsEachBookingAndTellsEachCustomer() {
        Booking ana = Booking.builder().id("b1").eventId("e1").customerEmail("ana@mail.ro").seats(2).build();
        Booking ion = Booking.builder().id("b2").eventId("e1").customerEmail("ion@mail.ro").seats(1).build();
        when(bookingRepository.findByEventIdOrderByBookedAt("e1")).thenReturn(List.of(ana, ion));

        bookingService.onEventCancelled(new EventCancelled("e1", "Rock Night", LocalDateTime.of(2030, 6, 12, 20, 0)));

        verify(bookingRepository).deleteAll(List.of(ana, ion));
        verify(bookingEventPublisher).publish(new BookingCancelled("b1", "e1", "ana@mail.ro", 2, EVENT_CANCELLED));
        verify(bookingEventPublisher).publish(new BookingCancelled("b2", "e1", "ion@mail.ro", 1, EVENT_CANCELLED));
    }

    @Test
    void changedEventIsAnnouncedToEachCustomer() {
        Booking ana = Booking.builder().id("b1").eventId("e1").customerEmail("ana@mail.ro").seats(2).build();
        Booking ion = Booking.builder().id("b2").eventId("e1").customerEmail("ion@mail.ro").seats(1).build();
        when(bookingRepository.findByEventIdOrderByBookedAt("e1")).thenReturn(List.of(ana, ion));
        LocalDateTime newDate = LocalDateTime.of(2030, 7, 1, 20, 0);

        bookingService.onEventChanged(new EventChanged("e1", "Rock Night", "BT Arena", "Cluj", newDate));

        verify(bookingEventPublisher).publish(
                new BookedEventChanged("b1", "e1", "ana@mail.ro", 2, "Rock Night", "BT Arena", "Cluj", newDate));
        verify(bookingEventPublisher).publish(
                new BookedEventChanged("b2", "e1", "ion@mail.ro", 1, "Rock Night", "BT Arena", "Cluj", newDate));
    }
}
