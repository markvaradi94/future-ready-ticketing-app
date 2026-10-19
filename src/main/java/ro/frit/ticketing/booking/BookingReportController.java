package ro.frit.ticketing.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/bookings/seats-per-event")
@RequiredArgsConstructor
public class BookingReportController {
    private final BookingService bookingService;

    @GetMapping
    public Map<String, Integer> seatsPerEvent() {
        return bookingService.bookedSeatsPerEventName();
    }

    @GetMapping("/fetch-join")
    public Map<String, Integer> seatsPerEventFetchJoin() {
        return bookingService.bookedSeatsPerEventNameFetchJoin();
    }
}
