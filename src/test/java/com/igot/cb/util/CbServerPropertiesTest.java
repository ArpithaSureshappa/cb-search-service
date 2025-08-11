package com.igot.cb.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CbServerPropertiesTest {

    @Test
    void testGetterAndSetter() {
        CbServerProperties properties = new CbServerProperties();

        // Test setter
        properties.setRecentSearchesLimit(10);

        // Test getter
        assertEquals(10, properties.getRecentSearchesLimit());
    }
}
