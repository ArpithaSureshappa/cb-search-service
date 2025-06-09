package com.igot.cb.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class PayloadValidationTest {

    private PayloadValidation payloadValidation;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        payloadValidation = new PayloadValidation();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testValidatePayload_success() throws Exception {
        // Arrange
        InputStream schemaStream = getClass().getResourceAsStream("/test-schema.json");
        assertNotNull(schemaStream, "Test schema should be present in resources as /test-schema.json");

        String validJson = """
            {
              "name": "Alice",
              "age": 30
            }
            """;

        JsonNode payload = objectMapper.readTree(validJson);

        // Act & Assert
        assertDoesNotThrow(() -> payloadValidation.validatePayload("/test-schema.json", payload));
    }

    @Test
    void testValidatePayload_validationErrors() throws Exception {
        // Arrange
        InputStream schemaStream = getClass().getResourceAsStream("/test-schema.json");
        assertNotNull(schemaStream);

        String invalidJson = """
            {
              "name": 123,
              "age": "thirty"
            }
            """;

        JsonNode payload = objectMapper.readTree(invalidJson);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () ->
                payloadValidation.validatePayload("/test-schema.json", payload));

        assertTrue(exception.getMessage().contains("Validation error(s):"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatusCode());
    }

    @Test
    void testValidatePayload_schemaNotFound() throws Exception {
        JsonNode payload = objectMapper.readTree("""
            { "any": "data" }
        """);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () ->
                payloadValidation.validatePayload("/non-existent-schema.json", payload));

        assertTrue(exception.getCode().contains("Failed to validate payload"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatusCode());
    }
}
