package com.igot.cb.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class   RestExceptionHandlingTest {

    private RestExceptionHandling exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new RestExceptionHandling();
    }

    @Test
    void testHandleGenericException() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<?> responseEntity = exceptionHandler.handleException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        ErrorResponse body = (ErrorResponse) responseEntity.getBody();
        assertNotNull(body);
        assertEquals("Something went wrong", body.getMessage());
        assertEquals("ERROR", body.getCode());
        assertEquals(500, body.getHttpStatusCode());
    }

    @Test
    void testHandleCustomExceptionWithStatus() {
        CustomException customException = new CustomException("Validation Error", "Invalid input", HttpStatus.BAD_REQUEST);

        ResponseEntity<?> responseEntity = exceptionHandler.handleException(customException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        ErrorResponse body = (ErrorResponse) responseEntity.getBody();
        assertNotNull(body);
        assertEquals("Invalid input", body.getMessage());
        assertEquals("Validation Error", body.getCode());
        assertEquals(400, body.getHttpStatusCode());
    }

}
