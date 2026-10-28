package ro.frit.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Locale.ENGLISH;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static ro.frit.notification.Notification.Fields.notifiedAt;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final DateTimeFormatter STARTS_AT = DateTimeFormatter.ofPattern("d MMM yyyy 'at' HH:mm", ENGLISH);

    private final NotificationRepository notificationRepository;

    public void store(String messageId, BookingConfirmed event) {
        Notification notification = Notification.builder()
                .messageId(messageId)
                .bookingId(event.bookingId())
                .customerEmail(event.customerEmail())
                .text(text(event))
                .notifiedAt(LocalDateTime.now().truncatedTo(MILLIS))
                .sent(false)
                .build();

        insertOnce(notification);
    }

    public void store(String messageId, BookingCancelled event) {
        Notification notification = Notification.builder()
                .messageId(messageId)
                .bookingId(event.bookingId())
                .customerEmail(event.customerEmail())
                .text(text(event))
                .notifiedAt(LocalDateTime.now().truncatedTo(MILLIS))
                .sent(false)
                .build();

        insertOnce(notification);
    }

    // Added after the session (stretch 3).
    public void store(String messageId, BookedEventChanged event) {
        Notification notification = Notification.builder()
                .messageId(messageId)
                .bookingId(event.bookingId())
                .customerEmail(event.customerEmail())
                .text(text(event))
                .notifiedAt(LocalDateTime.now().truncatedTo(MILLIS))
                .sent(false)
                .build();

        insertOnce(notification);
    }

    public List<Notification> findAll() {
        return notificationRepository.findAll(Sort.by(DESC, notifiedAt));
    }

    private static String text(BookingConfirmed event) {
        return "%d seat(s) for %s on %s are confirmed.".formatted(
                event.seats(), event.eventName(), STARTS_AT.format(event.eventStartsAt()));
    }

    private static String text(BookingCancelled event) {
        return switch (event.reason()) {
            case BY_CUSTOMER -> "%d seat(s) for event %s are cancelled, as you asked.".formatted(event.seats(), event.eventId());
            case EVENT_CANCELLED -> "%d seat(s) for event %s are cancelled: the event was called off.".formatted(event.seats(), event.eventId());
        };
    }

    private static String text(BookedEventChanged event) {
        return "Your event changed: %d seat(s) for %s, now at %s, %s, on %s.".formatted(
                event.seats(), event.eventName(), event.venueName(), event.city(), STARTS_AT.format(event.eventStartsAt()));
    }

    // The unique index on messageId refuses a message delivered again. Spring translates MongoDB's E11000 into its
    // own DuplicateKeyException (org.springframework.dao), the one to catch: MongoDB's never reaches us.
    private void insertOnce(Notification notification) {
        try {
            notificationRepository.insert(notification);
        } catch (DuplicateKeyException e) {
            log.debug("Message {} was already handled; duplicate delivery ignored", notification.getMessageId());
        }
    }
}
