package ro.frit.ticketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods
@ConfigurationPropertiesScan
public class TicketingApplication {

    static void main(String[] args) {
        SpringApplication.run(TicketingApplication.class, args);
    }
}