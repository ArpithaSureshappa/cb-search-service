package com.igot.cb.transactional.cassandrautils;

import com.datastax.oss.driver.api.core.*;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.igot.cb.exceptions.CustomException;
import com.igot.cb.util.Constants;
import com.igot.cb.util.PropertiesCache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;



import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CassandraConnectionManagerImplTest {

    @Mock
    PropertiesCache propertiesCache;

    @Mock
    CqlSession mockSession;

    @Mock
    Metadata mockMetadata;

    @Mock
    KeyspaceMetadata mockKeyspaceMetadata;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetConsistencyLevel_valid() {
        try (MockedStatic<PropertiesCache> staticMock = mockStatic(PropertiesCache.class)) {
            staticMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            when(propertiesCache.readProperty(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL))
                    .thenReturn("LOCAL_QUORUM");

            ConsistencyLevel level = invokeGetConsistencyLevel();
            assertEquals(DefaultConsistencyLevel.LOCAL_QUORUM, level);
        }
    }

    @Test
    void testGetConsistencyLevel_invalid() {
        try (MockedStatic<PropertiesCache> staticMock = mockStatic(PropertiesCache.class)) {
            staticMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            when(propertiesCache.readProperty(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL))
                    .thenReturn("INVALID");

            ConsistencyLevel level = invokeGetConsistencyLevel();
            assertNull(level);
        }
    }

    @Test
    void testShutdownHook() {
        Thread thread = new CassandraConnectionManagerImpl.ResourceCleanUp();
        thread.start();
    }

    private ConsistencyLevel invokeGetConsistencyLevel() {
        try {
            Method method = CassandraConnectionManagerImpl.class.getDeclaredMethod("getConsistencyLevel");
            method.setAccessible(true);
            return (ConsistencyLevel) method.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testConstructorThrowsException_whenHostIsBlank() {
        try (
                MockedStatic<PropertiesCache> propertiesCacheStatic = Mockito.mockStatic(PropertiesCache.class)
        ) {
            // Arrange
            PropertiesCache mockPropertiesCache = mock(PropertiesCache.class);
            propertiesCacheStatic.when(PropertiesCache::getInstance).thenReturn(mockPropertiesCache);
            when(mockPropertiesCache.getProperty(Constants.CASSANDRA_CONFIG_HOST)).thenReturn(" ");

            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, CassandraConnectionManagerImpl::new);
            assertEquals("Cassandra host is not configured", exception.getMessage());
        }
    }
    
    @Test
    void testResourceCleanup() {
        // This is just for code coverage
        CassandraConnectionManagerImpl.ResourceCleanUp cleanup = new CassandraConnectionManagerImpl.ResourceCleanUp();
        cleanup.run();
    }

}