package com.igot.cb.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionTest {

    @Test
    void testDefaultConstructor() {
        CustomException exception = new CustomException();

        assertNotNull(exception, "Exception instance should not be null");
        assertNull(exception.getCode(), "Code should be null by default");
        assertNull(exception.getMessage(), "Message should be null by default");
        assertNull(exception.getHttpStatusCode(), "HttpStatusCode should be null by default");
    }

    @Test
    void testParameterizedConstructor() {
        String code = "ERROR_CODE";
        String message = "An error occurred";
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

        CustomException exception = new CustomException(code, message, httpStatus);

        assertNotNull(exception, "Exception instance should not be null");
        assertEquals(code, exception.getCode(), "Code should match the input");
        assertEquals(message, exception.getMessage(), "Message should match the input");
        assertEquals(httpStatus, exception.getHttpStatusCode(), "HttpStatusCode should match the input");
    }

    @Test
    void testSettersAndGetters() {
        CustomException exception = new CustomException();

        String code = "ERROR_CODE";
        String message = "An error occurred";
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        exception.setCode(code);
        exception.setMessage(message);
        exception.setHttpStatusCode(httpStatus);

        assertEquals(code, exception.getCode(), "Code should match the set value");
        assertEquals(message, exception.getMessage(), "Message should match the set value");
        assertEquals(httpStatus, exception.getHttpStatusCode(), "HttpStatusCode should match the set value");
    }
}