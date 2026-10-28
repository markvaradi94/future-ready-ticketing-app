package ro.frit.ticketing;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import ro.frit.ticketing.booking.BookingEventPublisher;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

// The rules of Session 5, checked by reading the imports until now, written as tests: the build fails when one breaks.
@AnalyzeClasses(packages = "ro.frit.ticketing", importOptions = DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule onlyTheAdapterKnowsTheBroker = noClasses()
            .that().doNotImplement(BookingEventPublisher.class)
            .and().resideOutsideOfPackage("..config..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.amqp..")
            .because("the booking rules say what they need through the port; RabbitMQ is the adapter's business");

    @ArchTest
    static final ArchRule eventsDoNotKnowBookings = noClasses()
            .that().resideInAPackage("..ticketing.event..")
            .should().dependOnClassesThat().resideInAPackage("..ticketing.booking..")
            .because("booking may ask the event feature, never the other way round");

    @ArchTest
    static final ArchRule controllersDoNotUseRepositories = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
            .because("a controller talks to its service; the service owns the data");
}
