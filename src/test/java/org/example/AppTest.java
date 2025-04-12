package org.example;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class AppTest {
    @Test
    void hasGreeting() {
        final App classUnderTest = new App();
        assertNotNull(classUnderTest.getGreeting(), "app should have a greeting");
    }

    @Test
    void canRunMain() {
        final String[] args = {};
        App.main(args);
    }
}
