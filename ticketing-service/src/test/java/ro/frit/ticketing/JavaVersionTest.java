package ro.frit.ticketing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavaVersionTest {

    @Test
    void runsOnJava25() {
        assertEquals(25, Runtime.version().feature());
    }
}