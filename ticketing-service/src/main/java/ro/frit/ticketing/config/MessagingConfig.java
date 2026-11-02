package ro.frit.ticketing.config;

import org.slf4j.MDC;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ro.frit.ticketing.shared.web.CorrelationIdFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class MessagingConfig {
    public static final String EXCHANGE = "ticketing.events";
    public static final String BOOKING_CONFIRMED = "booking.confirmed";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String BOOKED_EVENT_CHANGED = "booked.event-changed";

    @Bean
    TopicExchange ticketingExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
        converter.setCreateMessageIds(true);
        return converter;
    }

    // The request's correlation id travels with every message, as a header under the same name as the HTTP header:
    // one place, so all three facts carry it.
    @Bean
    RabbitTemplateCustomizer correlationIdHeader() {
        return template -> template.addBeforePublishPostProcessors(message -> {
            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (correlationId != null) {
                message.getMessageProperties().setHeader(CorrelationIdFilter.HEADER, correlationId);
            }
            return message;
        });
    }
}
