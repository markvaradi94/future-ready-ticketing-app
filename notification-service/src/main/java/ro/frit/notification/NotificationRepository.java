package ro.frit.notification;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    Optional<Notification> findByMessageId(String messageId);
}
