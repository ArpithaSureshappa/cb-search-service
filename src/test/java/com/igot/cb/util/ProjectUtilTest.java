package com.igot.cb.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProjectUtilTest {

    @Test
    void testCreateDefaultResponse() {
        String api = "testApi";

        ApiResponse response = ProjectUtil.createDefaultResponse(api);

        assertNotNull(response, "Response should not be null");
        assertEquals(api, response.getId(), "API ID should match the input");
        assertEquals(Constants.API_VERSION_1, response.getVer(), "Version should match the constant");
        assertNotNull(response.getParams(), "Params should not be null");
        assertEquals(Constants.SUCCESS, response.getParams().getStatus(), "Status should be SUCCESS");
        assertEquals(HttpStatus.OK, response.getResponseCode(), "Response code should be OK");
        assertNotNull(response.getTs(), "Timestamp should not be null");

        // Validate UUID format for params ID
        assertDoesNotThrow(() -> UUID.fromString(response.getParams().getResMsgId()), "ResMsgId should be a valid UUID");

        // Validate timestamp format
        assertDoesNotThrow(() -> LocalDateTime.parse(response.getTs(), DateTimeFormatter.ISO_DATE_TIME), "Timestamp should be in ISO format");
    }
}