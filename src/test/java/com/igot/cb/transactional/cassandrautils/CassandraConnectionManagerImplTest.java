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
import java.util.Map;


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

    @Test
    void testGetSessionReturnsCachedSession() {
        try (MockedStatic<PropertiesCache> staticMock = mockStatic(PropertiesCache.class)) {

            // Mock PropertiesCache before instantiating the class
            PropertiesCache mockCache = mock(PropertiesCache.class);
            staticMock.when(PropertiesCache::getInstance).thenReturn(mockCache);

            // Provide dummy values so constructor doesn't NPE
            when(mockCache.getProperty(Constants.CASSANDRA_CONFIG_HOST)).thenReturn("127.0.0.1");
            when(mockCache.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_LOCAL)).thenReturn("1");
            when(mockCache.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_REMOTE)).thenReturn("1");
            when(mockCache.getProperty(Constants.HEARTBEAT_INTERVAL)).thenReturn("30");
            when(mockCache.readProperty(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn("LOCAL_ONE");

            // Mock the CqlSession builder to avoid real connections
            try (MockedStatic<CqlSession> sessionStatic = mockStatic(CqlSession.class)) {
                CqlSessionBuilder mockBuilder = mock(CqlSessionBuilder.class, RETURNS_SELF);
                CqlSession dummySession = mock(CqlSession.class);
                when(dummySession.getMetadata()).thenReturn(mock(Metadata.class));
                when(mockBuilder.build()).thenReturn(dummySession);
                sessionStatic.when(CqlSession::builder).thenReturn(mockBuilder);

                // Now instantiate safely
                CassandraConnectionManagerImpl manager = new CassandraConnectionManagerImpl();

                // Insert a mock session into the cache
                CqlSession mockCqlSession = mock(CqlSession.class);
                when(mockCqlSession.isClosed()).thenReturn(false);

                Map<String, CqlSession> sessionMap = (Map<String, CqlSession>)
                        getFieldValue(CassandraConnectionManagerImpl.class, "cassandraSessionMap");
                sessionMap.put("testks", mockCqlSession);

                // Act
                CqlSession result = manager.getSession("testks");

                // Assert
                assertEquals(mockCqlSession, result);
            }
        }
    }


    @Test
    void testGetSessionCreatesNewSessionWhenCacheEmpty() {
        try (MockedStatic<PropertiesCache> staticMock = mockStatic(PropertiesCache.class);
             MockedStatic<CqlSession> sessionStatic = mockStatic(CqlSession.class)) {

            // Arrange
            PropertiesCache mockProperties = mock(PropertiesCache.class);
            staticMock.when(PropertiesCache::getInstance).thenReturn(mockProperties);
            when(mockProperties.getProperty(Constants.CASSANDRA_CONFIG_HOST)).thenReturn("127.0.0.1");
            when(mockProperties.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_LOCAL)).thenReturn("1");
            when(mockProperties.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_REMOTE)).thenReturn("1");
            when(mockProperties.getProperty(Constants.HEARTBEAT_INTERVAL)).thenReturn("30");
            when(mockProperties.readProperty(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn("LOCAL_ONE");

            // Mock session builder
            CqlSessionBuilder mockBuilder = mock(CqlSessionBuilder.class, RETURNS_SELF);
            CqlSession mockBuiltSession = mock(CqlSession.class);
            when(mockBuiltSession.getMetadata()).thenReturn(mock(Metadata.class));
            when(mockBuilder.build()).thenReturn(mockBuiltSession);
            sessionStatic.when(CqlSession::builder).thenReturn(mockBuilder);

            CassandraConnectionManagerImpl manager = new CassandraConnectionManagerImpl();

            // Act
            CqlSession result = manager.getSession("newks");

            // Assert
            assertNotNull(result);
            verify(mockBuilder).withKeyspace("newks");
        }
    }

    @Test
    void testCreateCassandraConnectionWithNullKeyspace() {
        try (MockedStatic<PropertiesCache> staticMock = mockStatic(PropertiesCache.class);
             MockedStatic<CqlSession> sessionStatic = mockStatic(CqlSession.class)) {

            PropertiesCache mockProperties = mock(PropertiesCache.class);
            staticMock.when(PropertiesCache::getInstance).thenReturn(mockProperties);
            when(mockProperties.getProperty(Constants.CASSANDRA_CONFIG_HOST)).thenReturn("127.0.0.1");
            when(mockProperties.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_LOCAL)).thenReturn("1");
            when(mockProperties.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_REMOTE)).thenReturn("1");
            when(mockProperties.getProperty(Constants.HEARTBEAT_INTERVAL)).thenReturn("30");
            when(mockProperties.readProperty(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn("LOCAL_ONE");

            CqlSessionBuilder mockBuilder = mock(CqlSessionBuilder.class, RETURNS_SELF);
            CqlSession mockBuiltSession = mock(CqlSession.class);
            when(mockBuiltSession.getMetadata()).thenReturn(mock(Metadata.class));
            when(mockBuilder.build()).thenReturn(mockBuiltSession);
            sessionStatic.when(CqlSession::builder).thenReturn(mockBuilder);

            CassandraConnectionManagerImpl manager = new CassandraConnectionManagerImpl();

            // Act
            CqlSession result = invokeCreateConnectionWithKeyspaces(manager, null);

            // Assert
            assertNotNull(result);
            verify(mockBuilder, never()).withKeyspace((CqlIdentifier) any());
        }
    }

    @Test
    void testCreateCassandraConnectionWithNonNullKeyspace() {
        try (MockedStatic<PropertiesCache> staticMock = mockStatic(PropertiesCache.class);
             MockedStatic<CqlSession> sessionStatic = mockStatic(CqlSession.class)) {

            PropertiesCache mockProperties = mock(PropertiesCache.class);
            staticMock.when(PropertiesCache::getInstance).thenReturn(mockProperties);
            when(mockProperties.getProperty(Constants.CASSANDRA_CONFIG_HOST)).thenReturn("127.0.0.1");
            when(mockProperties.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_LOCAL)).thenReturn("1");
            when(mockProperties.getProperty(Constants.CORE_CONNECTIONS_PER_HOST_FOR_REMOTE)).thenReturn("1");
            when(mockProperties.getProperty(Constants.HEARTBEAT_INTERVAL)).thenReturn("30");
            when(mockProperties.readProperty(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn("LOCAL_ONE");

            CqlSessionBuilder mockBuilder = mock(CqlSessionBuilder.class, RETURNS_SELF);
            CqlSession mockBuiltSession = mock(CqlSession.class);
            when(mockBuiltSession.getMetadata()).thenReturn(mock(Metadata.class));
            when(mockBuilder.build()).thenReturn(mockBuiltSession);
            sessionStatic.when(CqlSession::builder).thenReturn(mockBuilder);

            CassandraConnectionManagerImpl manager = new CassandraConnectionManagerImpl();

            // Act
            CqlSession result = invokeCreateConnectionWithKeyspaces(manager, "testks");

            // Assert
            assertNotNull(result);
            verify(mockBuilder).withKeyspace("testks");
        }
    }

    // Helper to reflectively call private method
    private CqlSession invokeCreateConnectionWithKeyspaces(CassandraConnectionManagerImpl manager, String keyspace) {
        try {
            Method method = CassandraConnectionManagerImpl.class
                    .getDeclaredMethod("createCassandraConnectionWithKeySpaces", String.class);
            method.setAccessible(true);
            return (CqlSession) method.invoke(manager, keyspace);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helper for accessing static map
    @SuppressWarnings("unchecked")
    private Object getFieldValue(Class<?> clazz, String fieldName) {
        try {
            var field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
