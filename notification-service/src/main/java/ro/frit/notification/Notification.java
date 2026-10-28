package ro.frit.notification;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
@FieldNameConstants
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;

    @Indexed(unique = true)
    private String messageId;

    private String bookingId;
    private String customerEmail;
    private String text;
    private LocalDateTime notifiedAt;
    private boolean sent;
}