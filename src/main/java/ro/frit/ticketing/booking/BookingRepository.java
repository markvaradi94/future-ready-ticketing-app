package ro.frit.ticketing.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {

    // One select: the bookings joined with their events.
    @Query("select b from Booking b join fetch b.event")
    List<Booking> findAllWithEvent();
}
