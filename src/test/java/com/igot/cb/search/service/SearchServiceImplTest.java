package com.igot.cb.search.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.util.CbServerProperties;
import com.igot.cb.util.Constants;
import com.igot.cb.util.redis.cache.CacheService;

import org.igot.common.ApiResponse;
import org.igot.common.auth.AccessTokenValidator;
import org.igot.common.cassandra.CassandraOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.*;

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

    @Mock
    private CbServerProperties cbServerProperties;

    @Test
    void createUserRecentSearches_savesSearchSuccessfully() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";
        JsonNode searchQuery = new ObjectMapper().readTree("{\"nlpSearchQuery\":\"query1\",\"searchCategory\":\"category1\",\"searchQuery\":\"query1\"}");

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);
        when(cassandraOperation.getRecordsByProperties(
                eq(Constants.KEYSPACE_SUNBIRD_COURSES),
                eq(Constants.TABLE_USER_RECENT_SEARCH),
                anyMap(),
                isNull(),
                isNull()
        )).thenReturn(Collections.emptyList());
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

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(null);

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQuery, token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void readUserRecentSearches_returnsCachedSearches() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";
        String cachedJson = "[{\"searchQuery\":\"query1\"}]";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);
        when(cacheService.getCache(userId)).thenReturn(cachedJson);
        List<Map<String, Object>> expectedList = List.of(Map.of("searchQuery", "query1"));

        doReturn(expectedList).when(objectMapper).readValue(eq(cachedJson), any(TypeReference.class));

        ApiResponse response = searchServiceImpl.readUserRecentSearches(token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertNotNull(response.getResult().get("searchQueries"));
    }

    @Test
    void deleteUserAllRecentSearches_deletesSuccessfully() {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("user_id", userId);
        recordMap.put(Constants.TIMESTAMP, 123456789L);
        recordMap.put("search_query", "test");
        recordMap.put(Constants.IS_ACTIVE, true);
        List<Map<String, Object>> records = List.of(recordMap);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull())).thenReturn(records);

        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap()))
                .thenReturn(Collections.singletonMap(Constants.RESPONSE, Constants.SUCCESS));

        ApiResponse response = searchServiceImpl.deleteUserAllRecentSearches(token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals("All recent searches deleted successfully", response.getResult().get("result"));

        verify(cacheService).deleteCache(userId);
    }


    @Test
    void deleteUserRecentSearchesByTimestamp_deletesSuccessfully() {
        String token = "validToken";
        String userId = "user123";
        Long timestamp = 123456789L;

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("user_id", userId);
        recordMap.put(Constants.TIMESTAMP, timestamp);
        recordMap.put("search_query", "AI");
        recordMap.put("nlp_search_query", "Artificial Intelligence");
        recordMap.put("search_category", Set.of("course"));
        recordMap.put(Constants.IS_ACTIVE, true);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull())
        ).thenReturn(List.of(recordMap));

        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap()))
                .thenReturn(Collections.singletonMap(Constants.RESPONSE, Constants.SUCCESS));

        ApiResponse response = searchServiceImpl.deleteUserRecentSearchesByTimestamp(token, timestamp);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals("Recent search deleted successfully", response.getResult().get("result"));

        verify(cacheService).deleteCache(userId);
    }


    @Test
    void createUserRecentSearches_returnsErrorWhenSearchQueryIsNull() {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

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

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQuery, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("One or more required fields (nlpSearchQuery, searchCategory, searchQuery) are missing or empty", response.getParams().getErrMsg());
    }

    @Test
    void readUserRecentSearches_returnsErrorWhenTokenIsInvalid() {
        String token = "invalidToken";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(null);

        ApiResponse response = searchServiceImpl.readUserRecentSearches(token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void readUserRecentSearches_returnsEmptyWhenNoSearchesExist() {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);
        when(cacheService.getCache(userId)).thenReturn(null);
        when(cbServerProperties.getRecentSearchesLimit()).thenReturn(10);
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), isNull(), anyInt()))
                .thenReturn(Collections.emptyList());

        ApiResponse response = searchServiceImpl.readUserRecentSearches(token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals("User dont have any recent searches", response.getParams().getErrMsg());
    }

    @Test
    void deleteUserAllRecentSearches_returnsErrorWhenTokenIsInvalid() {
        String token = "invalidToken";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(null);

        ApiResponse response = searchServiceImpl.deleteUserAllRecentSearches(token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void deleteUserRecentSearchesByTimestamp_returnsErrorWhenTokenIsInvalid() {
        String token = "invalidToken";
        Long timestamp = 123456789L;

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(null);

        ApiResponse response = searchServiceImpl.deleteUserRecentSearchesByTimestamp(token, timestamp);

        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void testCreateUserRecentSearches_CoversForLoop() throws JsonProcessingException {
        String token = "valid-token";
        String userId = "user123";
        String actualQuery = "java course";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        Map<String, Object> existingRecord = new HashMap<>();
        existingRecord.put(Constants.SEARCH_QUERY, actualQuery);
        existingRecord.put(Constants.IS_ACTIVE, true);
        existingRecord.put(Constants.TIMESTAMP, 123456789L);
        existingRecord.put(Constants.SEARCH_CATEGORY, new HashSet<>(Arrays.asList("oldCategory")));

        List<Map<String, Object>> existingSearches = Collections.singletonList(existingRecord);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull()
        )).thenReturn(existingSearches);

        ApiResponse insertResponse = ApiResponse.createDefaultResponse(Constants.API_RECENT_SEARCH_CREATE);
        insertResponse.put(Constants.RESPONSE, Constants.SUCCESS);
        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap()))
                .thenReturn(insertResponse);

        ObjectNode searchQueryJson = new ObjectMapper().createObjectNode();
        searchQueryJson.put(Constants.NLP_SEARCH_QUERY_KEY, "nlp text");
        searchQueryJson.put(Constants.SEARCH_CATEGORY_KEY, "newCategory");
        searchQueryJson.put(Constants.SEARCH_QUERY_KEY, actualQuery);

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQueryJson, token);

        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        verify(cassandraOperation, times(1)).deleteRecord(anyString(), anyString(), anyMap());
        verify(cacheService, times(1)).deleteCache(userId);
    }

    @Test
    void deleteUserRecentSearchesByTimestamp_deletesFailed() {
        String token = "validToken";
        String userId = "user123";
        Long timestamp = 123456789L;

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull())
        ).thenReturn(Collections.emptyList());

        ApiResponse response = searchServiceImpl.deleteUserRecentSearchesByTimestamp(token, timestamp);

        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
        assertEquals("No recent search found for the given userid and timestamp", response.getParams().getErrMsg());
    }

    @Test
    void createUserRecentSearches_withEmptySearchQuery() {
        String token = "validToken";
        String userId = "user123";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode searchQuery = mapper.createObjectNode();

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQuery, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void createUserRecentSearches_withExistingInactiveRecord() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";
        String actualQuery = "python course";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        Map<String, Object> existingRecord = new HashMap<>();
        existingRecord.put(Constants.SEARCH_QUERY, actualQuery);
        existingRecord.put(Constants.IS_ACTIVE, false);
        existingRecord.put(Constants.TIMESTAMP, 123456789L);
        existingRecord.put(Constants.SEARCH_CATEGORY, new HashSet<>(Arrays.asList("oldCategory")));

        List<Map<String, Object>> existingSearches = Collections.singletonList(existingRecord);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull()
        )).thenReturn(existingSearches);

        ApiResponse insertResponse = ApiResponse.createDefaultResponse(Constants.API_RECENT_SEARCH_CREATE);
        insertResponse.put(Constants.RESPONSE, Constants.SUCCESS);
        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap()))
                .thenReturn(insertResponse);

        ObjectNode searchQueryJson = new ObjectMapper().createObjectNode();
        searchQueryJson.put(Constants.NLP_SEARCH_QUERY_KEY, "nlp text");
        searchQueryJson.put(Constants.SEARCH_CATEGORY_KEY, "newCategory");
        searchQueryJson.put(Constants.SEARCH_QUERY_KEY, actualQuery);

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQueryJson, token);

        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        verify(cassandraOperation, times(1)).deleteRecord(anyString(), anyString(), anyMap());
    }

    @Test
    void createUserRecentSearches_withWhitespaceInQuery() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull()
        )).thenReturn(Collections.emptyList());

        ApiResponse insertResponse = ApiResponse.createDefaultResponse(Constants.API_RECENT_SEARCH_CREATE);
        insertResponse.put(Constants.RESPONSE, Constants.SUCCESS);
        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap()))
                .thenReturn(insertResponse);

        ObjectNode searchQueryJson = new ObjectMapper().createObjectNode();
        searchQueryJson.put(Constants.NLP_SEARCH_QUERY_KEY, "nlp text");
        searchQueryJson.put(Constants.SEARCH_CATEGORY_KEY, "category");
        searchQueryJson.put(Constants.SEARCH_QUERY_KEY, "  java   course  ");

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQueryJson, token);

        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void createUserRecentSearches_withMixedCaseQuery() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull()
        )).thenReturn(Collections.emptyList());

        ApiResponse insertResponse = ApiResponse.createDefaultResponse(Constants.API_RECENT_SEARCH_CREATE);
        insertResponse.put(Constants.RESPONSE, Constants.SUCCESS);
        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap()))
                .thenReturn(insertResponse);

        ObjectNode searchQueryJson = new ObjectMapper().createObjectNode();
        searchQueryJson.put(Constants.NLP_SEARCH_QUERY_KEY, "nlp text");
        searchQueryJson.put(Constants.SEARCH_CATEGORY_KEY, "category");
        searchQueryJson.put(Constants.SEARCH_QUERY_KEY, "JaVa CoUrSe");

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQueryJson, token);

        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void createUserRecentSearches_insertRecordFails() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull()
        )).thenReturn(Collections.emptyList());

        ApiResponse insertResponse = ApiResponse.createDefaultResponse(Constants.API_RECENT_SEARCH_CREATE);
        insertResponse.put(Constants.RESPONSE, "failure");
        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap()))
                .thenReturn(insertResponse);

        ObjectNode searchQueryJson = new ObjectMapper().createObjectNode();
        searchQueryJson.put(Constants.NLP_SEARCH_QUERY_KEY, "nlp text");
        searchQueryJson.put(Constants.SEARCH_CATEGORY_KEY, "category");
        searchQueryJson.put(Constants.SEARCH_QUERY_KEY, "test query");

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQueryJson, token);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals("Failed to save search query.", response.getParams().getErrMsg());
    }

    @Test
    void createUserRecentSearches_withNullCategories() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";
        String actualQuery = "data science";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        Map<String, Object> existingRecord = new HashMap<>();
        existingRecord.put(Constants.SEARCH_QUERY, actualQuery);
        existingRecord.put(Constants.IS_ACTIVE, true);
        existingRecord.put(Constants.TIMESTAMP, 123456789L);
        existingRecord.put(Constants.SEARCH_CATEGORY, null);

        List<Map<String, Object>> existingSearches = Collections.singletonList(existingRecord);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull()
        )).thenReturn(existingSearches);

        ApiResponse insertResponse = ApiResponse.createDefaultResponse(Constants.API_RECENT_SEARCH_CREATE);
        insertResponse.put(Constants.RESPONSE, Constants.SUCCESS);
        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap()))
                .thenReturn(insertResponse);

        ObjectNode searchQueryJson = new ObjectMapper().createObjectNode();
        searchQueryJson.put(Constants.NLP_SEARCH_QUERY_KEY, "nlp text");
        searchQueryJson.put(Constants.SEARCH_CATEGORY_KEY, "newCategory");
        searchQueryJson.put(Constants.SEARCH_QUERY_KEY, actualQuery);

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQueryJson, token);

        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        verify(cassandraOperation, times(1)).deleteRecord(anyString(), anyString(), anyMap());
    }

    @Test
    void readUserRecentSearches_fromDatabaseWithResults() {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);
        when(cacheService.getCache(userId)).thenReturn(null);
        when(cbServerProperties.getRecentSearchesLimit()).thenReturn(10);

        Map<String, Object> searchRecord = new HashMap<>();
        searchRecord.put(Constants.SEARCH_QUERY, "test query");
        searchRecord.put(Constants.TIMESTAMP, 123456789L);
        List<Map<String, Object>> userSearchList = List.of(searchRecord);

        when(cassandraOperation.getRecordsByProperties(
                eq(Constants.KEYSPACE_SUNBIRD_COURSES),
                eq(Constants.TABLE_USER_RECENT_SEARCH),
                anyMap(),
                isNull(),
                eq(10)
        )).thenReturn(userSearchList);

        ApiResponse response = searchServiceImpl.readUserRecentSearches(token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertNotNull(response.getResult().get("searchQueries"));
        verify(cacheService).putCache(eq(userId), eq(userSearchList));
    }

    @Test
    void readUserRecentSearches_cacheThrowsException() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";
        String cachedJson = "[{\"searchQuery\":\"query1\"}]";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);
        when(cacheService.getCache(userId)).thenReturn(cachedJson);
        when(objectMapper.readValue(eq(cachedJson), any(TypeReference.class)))
                .thenThrow(new JsonProcessingException("Parse error") {
                    private static final long serialVersionUID = 1L;
                });

        try {
            searchServiceImpl.readUserRecentSearches(token);
        } catch (RuntimeException e) {
            assertNotNull(e);
            assertEquals(RuntimeException.class, e.getClass());
        }
    }

    @Test
    void deleteUserAllRecentSearches_withMultipleRecords() {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        Map<String, Object> record1 = new HashMap<>();
        record1.put("user_id", userId);
        record1.put(Constants.TIMESTAMP, 111111111L);
        record1.put("search_query", "test1");
        record1.put(Constants.IS_ACTIVE, true);

        Map<String, Object> record2 = new HashMap<>();
        record2.put("user_id", userId);
        record2.put(Constants.TIMESTAMP, 222222222L);
        record2.put("search_query", "test2");
        record2.put(Constants.IS_ACTIVE, true);

        List<Map<String, Object>> records = List.of(record1, record2);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull())).thenReturn(records);

        ApiResponse response = searchServiceImpl.deleteUserAllRecentSearches(token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        verify(cassandraOperation, times(2)).deleteRecord(anyString(), anyString(), anyMap());
        verify(cassandraOperation, times(2)).insertRecord(anyString(), anyString(), anyMap());
        verify(cacheService).deleteCache(userId);
    }

    @Test
    void deleteUserAllRecentSearches_withEmptyRecordsList() {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull())).thenReturn(Collections.emptyList());

        ApiResponse response = searchServiceImpl.deleteUserAllRecentSearches(token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        verify(cassandraOperation, never()).deleteRecord(anyString(), anyString(), anyMap());
        verify(cacheService).deleteCache(userId);
    }

    @Test
    void deleteUserRecentSearchesByTimestamp_withException() {
        String token = "validToken";
        String userId = "user123";
        Long timestamp = 123456789L;

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull())
        ).thenThrow(new RuntimeException("Database connection error"));

        ApiResponse response = searchServiceImpl.deleteUserRecentSearchesByTimestamp(token, timestamp);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("An error occurred while deleting the recent search", response.getParams().getErrMsg());
    }

    @Test
    void createUserRecentSearches_withMultipleDuplicateRecords() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";
        String actualQuery = "machine learning";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        Map<String, Object> record1 = new HashMap<>();
        record1.put(Constants.SEARCH_QUERY, actualQuery);
        record1.put(Constants.IS_ACTIVE, true);
        record1.put(Constants.TIMESTAMP, 111111111L);
        record1.put(Constants.SEARCH_CATEGORY, new HashSet<>(Arrays.asList("category1")));

        Map<String, Object> record2 = new HashMap<>();
        record2.put(Constants.SEARCH_QUERY, actualQuery);
        record2.put(Constants.IS_ACTIVE, true);
        record2.put(Constants.TIMESTAMP, 222222222L);
        record2.put(Constants.SEARCH_CATEGORY, new HashSet<>(Arrays.asList("category2")));

        List<Map<String, Object>> existingSearches = Arrays.asList(record1, record2);

        when(cassandraOperation.getRecordsByProperties(
                anyString(), anyString(), anyMap(), isNull(), isNull()
        )).thenReturn(existingSearches);

        ApiResponse insertResponse = ApiResponse.createDefaultResponse(Constants.API_RECENT_SEARCH_CREATE);
        insertResponse.put(Constants.RESPONSE, Constants.SUCCESS);
        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap()))
                .thenReturn(insertResponse);

        ObjectNode searchQueryJson = new ObjectMapper().createObjectNode();
        searchQueryJson.put(Constants.NLP_SEARCH_QUERY_KEY, "nlp text");
        searchQueryJson.put(Constants.SEARCH_CATEGORY_KEY, "category3");
        searchQueryJson.put(Constants.SEARCH_QUERY_KEY, actualQuery);

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQueryJson, token);

        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        verify(cassandraOperation, times(2)).deleteRecord(anyString(), anyString(), anyMap());
    }

    @Test
    void createUserRecentSearches_withMissingNlpSearchQuery() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        ObjectNode searchQueryJson = new ObjectMapper().createObjectNode();
        searchQueryJson.put(Constants.SEARCH_CATEGORY_KEY, "category");
        searchQueryJson.put(Constants.SEARCH_QUERY_KEY, "test query");

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQueryJson, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void createUserRecentSearches_withMissingSearchCategory() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        ObjectNode searchQueryJson = new ObjectMapper().createObjectNode();
        searchQueryJson.put(Constants.NLP_SEARCH_QUERY_KEY, "nlp text");
        searchQueryJson.put(Constants.SEARCH_QUERY_KEY, "test query");

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQueryJson, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void createUserRecentSearches_withMissingSearchQuery() throws JsonProcessingException {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.fetchUserIdFromAccessToken(eq(token), any(ApiResponse.class))).thenReturn(userId);

        ObjectNode searchQueryJson = new ObjectMapper().createObjectNode();
        searchQueryJson.put(Constants.NLP_SEARCH_QUERY_KEY, "nlp text");
        searchQueryJson.put(Constants.SEARCH_CATEGORY_KEY, "category");

        ApiResponse response = searchServiceImpl.createUserRecentSearches(searchQueryJson, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }
}
