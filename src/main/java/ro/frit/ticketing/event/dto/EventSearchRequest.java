package ro.frit.ticketing.event.dto;

import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

// The query parameters of GET /events, bound by name. A parameter that is not sent stays null.
@FieldNameConstants
public record EventSearchRequest(String city,
                                 String name,
                                 LocalDateTime from,
                                 LocalDateTime to) {
}
