package com.igot.cb.transactional.cassandrautils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CassandraConfigTest {

    @InjectMocks
    private TestCassandraConfig cassandraConfig;

    @BeforeEach
    void setUp() {
        cassandraConfig.setContactPoints("localhost");
        cassandraConfig.setPort(9042);
        cassandraConfig.setKeyspaceName("testKeyspace");
    }

    @Test
    void getKeyspaceName() {
        // Act
        String keyspaceName = cassandraConfig.getKeyspaceName();
        
        // Assert
        assertEquals("testKeyspace", keyspaceName);
    }

    @Test
    void getPort() {
        // Act
        int port = cassandraConfig.getPort();
        
        // Assert
        assertEquals(9042, port);
    }

    @Test
    void getContactPoints() {
        // Act
        String contactPoints = cassandraConfig.getContactPoints();
        
        // Assert
        assertEquals("localhost", contactPoints);
    }

    @Test
    void setPort() {
        // Act
        cassandraConfig.setPort(9043);
        
        // Assert
        assertEquals(9043, cassandraConfig.getPort());
    }

    @Test
    void setKeyspaceName() {
        // Act
        cassandraConfig.setKeyspaceName("newKeyspace");
        
        // Assert
        assertEquals("newKeyspace", cassandraConfig.getKeyspaceName());
    }

    // Test implementation of CassandraConfig for testing
    private static class TestCassandraConfig extends CassandraConfig {
        // No additional implementation needed
    }
}