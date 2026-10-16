package ro.frit.ticketing.booking;

import jakarta.validation.Valid;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps the bookings in memory for now. The method names are the ones Spring Data uses, so in Session 3 this class
 * becomes a {@code MongoRepository} interface and the service does not notice.
 * <p>
 * {@code @Validated} makes Spring check the {@code @Valid} parameter: saving a {@link Booking} that breaks one of its
 * constraints throws a {@code ConstraintViolationException}, and nothing is stored.
 */
@Repository
@Validated
public class BookingRepository {
    private final Map<String, Booking> bookings = new LinkedHashMap<>();

    public Booking save(@Valid Booking booking) {
        bookings.put(booking.getId(), booking);
        return booking;
    }

    public Optional<Booking> findById(String id) {
        return Optional.ofNullable(bookings.get(id));
    }

    public List<Booking> findAll() {
        return List.copyOf(bookings.values());
    }
}
