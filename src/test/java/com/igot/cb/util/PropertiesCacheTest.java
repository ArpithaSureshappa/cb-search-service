package com.igot.cb.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PropertiesCacheTest {

    private PropertiesCache propertiesCache;

    @BeforeEach
    void setUp() {
        propertiesCache = PropertiesCache.getInstance();
    }

    @Test
    void testSingletonInstance() {
        PropertiesCache instance1 = PropertiesCache.getInstance();
        PropertiesCache instance2 = PropertiesCache.getInstance();
        assertSame(instance1, instance2, "Instances should be the same (singleton)");
    }


    @Test
    void testGetPropertyFallbackToKey() {
        String key = "NON_EXISTENT_KEY";
        String result = propertiesCache.getProperty(key);
        assertEquals(key, result, "Should return the key itself if no value is found");
    }

    @Test
    void testReadPropertyFromConfig() throws Exception {
        String key = "application.name";
        String expectedValue = "MyApp";

        // Mock the internal properties
        Properties mockProperties = mock(Properties.class);
        when(mockProperties.getProperty(key)).thenReturn(expectedValue);

        // Use reflection to set the private properties field in PropertiesCache
        Field propertiesField = PropertiesCache.class.getDeclaredField("configProp");
        propertiesField.setAccessible(true);
        propertiesField.set(propertiesCache, mockProperties);

        String result = propertiesCache.readProperty(key);
        assertEquals(expectedValue, result, "Should return the value from properties file");
    }

    @Test
    void testReadPropertyReturnsNullForNonExistentKey() {
        String key = "NON_EXISTENT_KEY";
        String result = propertiesCache.readProperty(key);
        assertNull(result, "Should return null if no value is found");
    }
}
