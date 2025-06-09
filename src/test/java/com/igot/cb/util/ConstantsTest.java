package com.igot.cb.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstantsTest {

    @Test
    void testConstantsLogic() {
        // Add test logic here
        assertTrue(true);
    }

    @Test
    void testConstantValues() {
        assertEquals("success", Constants.SUCCESS);
        assertEquals("Failed", Constants.FAILED);
        assertEquals("Unauthorized", Constants.UNAUTHORIZED);
        // Add more assertions for other constants
    }
}