package com.dau.computerlab;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SmokeTest {

    @Test
    void testAddition() {
        assertEquals(4, 2 + 2);
    }

    @Test
    void testString() {
        assertEquals("FND-05", "FND-" + "05");
    }

    @Test
    void testBoolean() {
        assertTrue(10 > 5);
    }
}