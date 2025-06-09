package com.igot.cb.search.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.authentication.util.AccessTokenValidator;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import com.igot.cb.util.ApiResponse;
import com.igot.cb.util.Constants;
import com.igot.cb.util.redis.cache.CacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {
    @Mock
    private AccessTokenValidator accessTokenValidator;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private CacheService cacheService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SearchServiceImpl searchServiceImpl;

    @Test
    void createUserRecentSearches_savesSearchSuccessfully() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";
        JsonNode searchQuery = new ObjectMapper().readTree("{\"nlpSearchQuery\":\"query1\",\"searchCategory\":\"category1\",\"searchQuery\":\"query1\"}");

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByOrder(anyString(), anyString(), anyMap(), anyInt(), any()))
                .thenReturn(Collections.emptyList());
        ApiResponse mockApiResponse = new ApiResponse();
        mockApiResponse.put(Constants.RESPONSE, Constants.SUCCESS);

        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap()))
                .thenReturn(mockApiResponse);

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQuery, token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
    }

    @Test
    void createUserRecentSearches_returnsErrorWhenTokenIsInvalid() {
        String token = "invalidToken";
        JsonNode searchQuery = new ObjectMapper().createObjectNode();

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(Constants.UNAUTHORIZED);

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQuery, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(Constants.USER_ID_DOESNT_EXIST, response.getParams().getErrMsg());
    }

    @Test
    void readUserRecentSearches_returnsCachedSearches() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";
        String cachedJson = "[{\"searchQuery\":\"query1\"}]";

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);
        when(cacheService.getCache(userId)).thenReturn(cachedJson);
        List<Map<String, Object>> expectedList = List.of(Map.of("searchQuery", "query1"));

        when(cacheService.getCache(userId)).thenReturn(cachedJson);
        doReturn(expectedList).when(objectMapper).readValue(eq(cachedJson), any(TypeReference.class));

        ApiResponse response = searchServiceImpl.readUserRecentSearches(token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertNotNull(response.getResult().get("searchQueries"));
    }

    @Test
    void deleteUserAllRecentSearches_deletesSuccessfully() {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);
        Map<String, Object> deleteResponse = new HashMap<>();
        deleteResponse.put(Constants.RESPONSE, Constants.SUCCESS);
        when(cassandraOperation.deleteRecord(anyString(), anyString(), anyMap()))
                .thenReturn(deleteResponse);

        ApiResponse response = searchServiceImpl.deleteUserAllRecentSearches(token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
    }

    @Test
    void deleteUserRecentSearchesByTimestamp_deletesSuccessfully() {
        String token = "validToken";
        String userId = "user123";
        Long timestamp = 123456789L;

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);
        Map<String, Object> deleteResponse = new HashMap<>();
        deleteResponse.put(Constants.RESPONSE, Constants.SUCCESS);
        when(cassandraOperation.deleteRecord(anyString(), anyString(), anyMap()))
                .thenReturn(deleteResponse);

        ApiResponse response = searchServiceImpl.deleteUserRecentSearchesByTimestamp(token, timestamp);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
    }

    @Test
    void createUserRecentSearches_returnsErrorWhenSearchQueryIsNull() {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);

        ApiResponse response = searchServiceImpl.createUserRecentSearches(null, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("search query is empty", response.getParams().getErrMsg());
    }

    @Test
    void createUserRecentSearches_returnsErrorWhenRequiredFieldsAreMissing() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";
        JsonNode searchQuery = new ObjectMapper().readTree("{\"nlpSearchQuery\":\"\",\"searchCategory\":\"\",\"searchQuery\":\"\"}");

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQuery, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("One or more required fields (nlpSearchQuery, searchCategory, searchQuery) are missing or empty", response.getParams().getErrMsg());
    }

    @Test
    void readUserRecentSearches_returnsErrorWhenTokenIsInvalid() {
        String token = "invalidToken";

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(Constants.UNAUTHORIZED);

        ApiResponse response = searchServiceImpl.readUserRecentSearches(token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(Constants.USER_ID_DOESNT_EXIST, response.getParams().getErrMsg());
    }

    @Test
    void readUserRecentSearches_returnsEmptyWhenNoSearchesExist() {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByOrder(anyString(), anyString(), anyMap(), anyInt(), any()))
                .thenReturn(Collections.emptyList());

        ApiResponse response = searchServiceImpl.readUserRecentSearches(token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals("User dont have any recent searches", response.getParams().getErrMsg());
    }

    @Test
    void deleteUserAllRecentSearches_returnsErrorWhenTokenIsInvalid() {
        String token = "invalidToken";

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(Constants.UNAUTHORIZED);

        ApiResponse response = searchServiceImpl.deleteUserAllRecentSearches(token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(Constants.USER_ID_DOESNT_EXIST, response.getParams().getErrMsg());
    }

    @Test
    void deleteUserRecentSearchesByTimestamp_returnsErrorWhenTokenIsInvalid() {
        String token = "invalidToken";
        Long timestamp = 123456789L;

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(Constants.UNAUTHORIZED);

        ApiResponse response = searchServiceImpl.deleteUserRecentSearchesByTimestamp(token, timestamp);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(Constants.USER_ID_DOESNT_EXIST, response.getParams().getErrMsg());
    }

    @Test
    void deleteUserRecentSearchesByTimestamp_returnsErrorWhenDeleteFails() {
        String token = "validToken";
        String userId = "user123";
        Long timestamp = 123456789L;

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);
        when(cassandraOperation.deleteRecord(anyString(), anyString(), anyMap()))
                .thenReturn(Map.of(Constants.RESPONSE, Constants.FAILED, "errmsg", "Delete operation failed"));

        ApiResponse response = searchServiceImpl.deleteUserRecentSearchesByTimestamp(token, timestamp);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("Delete operation failed", response.getParams().getErrMsg());
    }
}
