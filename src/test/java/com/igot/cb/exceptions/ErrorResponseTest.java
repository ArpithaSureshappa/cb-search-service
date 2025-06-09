package com.igot.cb.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void testErrorResponseBuilder() {
        String code = "ERROR_CODE";
        String message = "An error occurred";
        int httpStatusCode = 400;

        // Build an ErrorResponse object
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(code)
                .message(message)
                .httpStatusCode(httpStatusCode)
                .build();

        // Assertions
        assertNotNull(errorResponse, "ErrorResponse should not be null");
        assertEquals(code, errorResponse.getCode(), "Code should match the input");
        assertEquals(message, errorResponse.getMessage(), "Message should match the input");
        assertEquals(httpStatusCode, errorResponse.getHttpStatusCode(), "HTTP status code should match the input");
    }

    @Test
    void testEqualsAndHashCode() {
        ErrorResponse response1 = ErrorResponse.builder()
                .code("CODE")
                .message("MSG")
                .httpStatusCode(404)
                .build();

        ErrorResponse response2 = ErrorResponse.builder()
                .code("CODE")
                .message("MSG")
                .httpStatusCode(404)
                .build();

        ErrorResponse response3 = ErrorResponse.builder()
                .code("DIFFERENT")
                .message("MSG")
                .httpStatusCode(500)
                .build();

        // Reflexive
        assertEquals(response1, response1);

        // Symmetric
        assertEquals(response1, response2);
        assertEquals(response2, response1);

        // Consistent hash codes
        assertEquals(response1.hashCode(), response2.hashCode());

        // Not equal
        assertNotEquals(response1, response3);
    }

    @Test
    void testToStringIsNotNull() {
        ErrorResponse response = ErrorResponse.builder()
                .code("500")
                .message("Internal Error")
                .httpStatusCode(500)
                .build();

        assertNotNull(response.toString());
        assertTrue(response.toString().contains("500"));
    }
}
