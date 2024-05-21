package org.dataverse.test.hello;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class HelloTest {
    @Test
    public void testMain() {
        String[] args = null;
        Hello.main(args);
        // just asserting that we got here
        assertTrue(true);
    }
}
