package com.igot.cb.transactional.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CassandraPropertyReaderExceptionTest {

    @Test
    void constructor_WithMessageAndCause_SetsMessageAndCause() {
        // Arrange
        String errorMessage = "Test error message";
        Throwable cause = new RuntimeException("Test cause");
        
        // Act
        CassandraPropertyReaderException exception = new CassandraPropertyReaderException(errorMessage, cause);
        
        // Assert
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}