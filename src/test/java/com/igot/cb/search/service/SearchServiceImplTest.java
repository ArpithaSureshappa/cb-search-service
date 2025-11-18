package com.igot.cb.search.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.authentication.util.AccessTokenValidator;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import com.igot.cb.util.ApiResponse;
import com.igot.cb.util.Constants;
import com.igot.cb.util.CbServerProperties;

import com.igot.cb.util.ProjectUtil;
import com.igot.cb.util.redis.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class SearchServiceImplTest {

    @InjectMocks
    private SearchServiceImpl searchService;

    @Mock
    private AccessTokenValidator accessTokenValidator;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private CacheService cacheService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CbServerProperties cbServerProperties;

    private ObjectMapper realMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // If searchService uses objectMapper injected, replace with real one for JsonNode creation where needed
        if (objectMapper == null) {
            objectMapper = realMapper;
        }
    }

    @Test
    void createUserRecentSearches_shouldSaveAndReturnSuccess() throws Exception {
        String token = "validToken";
        String userId = "user-123";
        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);

        // build search query json with the keys the service expects
        JsonNode searchQuery = realMapper.createObjectNode()
                .put(Constants.NLP_SEARCH_QUERY_KEY, "nlp text")
                .put(Constants.SEARCH_CATEGORY_KEY, "categoryA")
                .put(Constants.SEARCH_QUERY_KEY, "My Query");

        // no existing searches
        when(cassandraOperation.getRecordsByOrder(
                eq(Constants.KEYSPACE_SUNBIRD_COURSES),
                eq(Constants.TABLE_SEARCH_INDEX_BY_USER),
                anyMap(), isNull(), isNull()))
                .thenReturn(Collections.emptyList());

        // mock insert for search index by user -> return ApiResponse whose get(Constants.RESPONSE) = Constants.SUCCESS
        ApiResponse insertIndexResp = mock(ApiResponse.class);
        when(cassandraOperation.insertRecord(
                eq(Constants.KEYSPACE_SUNBIRD_COURSES),
                eq(Constants.TABLE_SEARCH_INDEX_BY_USER),
                anyMap(),
                any()))
                .thenReturn(insertIndexResp);
        when(insertIndexResp.get(Constants.RESPONSE)).thenReturn(Constants.SUCCESS);

        // user recent search insert - can return null or an ApiResponse, service doesn't inspect it beyond calling
        when(cassandraOperation.insertRecord(
                eq(Constants.KEYSPACE_SUNBIRD_COURSES),
                eq(Constants.TABLE_USER_RECENT_SEARCH),
                anyMap(),
                isNull()))
                .thenReturn(null);

        when(cbServerProperties.getIsAuditTableEntryEnabled()).thenReturn(false);

        ApiResponse response = searchService.createUserRecentSearches(searchQuery, token);

        assertNotNull(response);
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals("search request saved successfully", response.getParams().getErrMsg());
        // verify cache cleared
        verify(cacheService).deleteCache(userId);
    }

    @Test
    void createUserRecentSearches_returnsErrorWhenTokenIsInvalid() {
        String token = "invalidToken";
        JsonNode searchQuery = new ObjectMapper().createObjectNode();

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(Constants.UNAUTHORIZED);

        ApiResponse response = searchService.createUserRecentSearches(searchQuery, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(Constants.USER_ID_DOESNT_EXIST, response.getParams().getErrMsg());
    }

    @Test
    void readUserRecentSearches_returnsCachedResults_whenCachePresent() throws Exception {
        String token = "t";
        String userId = "user-1";
        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);

        // prepare cached JSON
        List<Map<String, Object>> cachedList = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put(Constants.USERID, userId);
        item.put(Constants.SEARCH_QUERY, "q");
        cachedList.add(item);

        String cachedJson = realMapper.writeValueAsString(cachedList);
        when(cacheService.getCache(userId)).thenReturn(cachedJson);

        // objectMapper used inside service to convert cached json -> result; provide a real mapper
        when(objectMapper.readValue(eq(cachedJson), ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>>any()))
                .thenReturn(cachedList);

        ApiResponse response = searchService.readUserRecentSearches(token);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertTrue(response.getResult().containsKey("searchQueries"));
        List<?> resultList = (List<?>) response.getResult().get("searchQueries");
        assertEquals(1, resultList.size());
    }

    @Test
    void deleteUserAllRecentSearches_shouldDeleteAllAndReturnOk() {
        String token = "t2";
        String userId = "user-2";
        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);

        // prepare records returned from Cassandra
        Map<String, Object> rec1 = new HashMap<>();
        rec1.put(Constants.USERID, userId);
        rec1.put(Constants.TIMESTAMP, Uuids.timeBased());
        List<Map<String, Object>> records = Collections.singletonList(rec1);

        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                eq(Constants.KEYSPACE_SUNBIRD_COURSES),
                eq(Constants.TABLE_USER_RECENT_SEARCH),
                anyMap(),
                isNull(),
                isNull()))
                .thenReturn(records);

        // simulate deleteRecord returning success
        Map<String, Object> deleteResult = new HashMap<>();
        deleteResult.put(Constants.RESPONSE, Constants.SUCCESS);
        when(cassandraOperation.deleteRecord(eq(Constants.KEYSPACE_SUNBIRD_COURSES), eq(Constants.TABLE_USER_RECENT_SEARCH), anyMap()))
                .thenReturn(deleteResult);

        when(cbServerProperties.getIsAuditTableEntryEnabled()).thenReturn(false);

        ApiResponse response = searchService.deleteUserAllRecentSearches(token);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertTrue(((Map<?, ?>) response.getResult()).get("result").toString().contains("deleted successfully"));
        verify(cacheService).deleteCache(userId);
    }

    @Test
    void deleteUserRecentSearchesByTimestamp_shouldRemoveSingleRecord() {
        String token = "t3";
        String userId = "user-3";
        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);

        UUID ts = Uuids.timeBased();
        Map<String, Object> record = new HashMap<>();
        record.put(Constants.USERID, userId);
        record.put(Constants.TIMESTAMP, ts);

        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                eq(Constants.KEYSPACE_SUNBIRD_COURSES),
                eq(Constants.TABLE_USER_RECENT_SEARCH),
                anyMap(),
                isNull(),
                isNull()))
                .thenReturn(Collections.singletonList(record));

        Map<String, Object> deleteResp = new HashMap<>();
        deleteResp.put(Constants.RESPONSE, Constants.SUCCESS);
        when(cassandraOperation.deleteRecord(eq(Constants.KEYSPACE_SUNBIRD_COURSES), eq(Constants.TABLE_USER_RECENT_SEARCH), anyMap()))
                .thenReturn(deleteResp);

        when(cbServerProperties.getIsAuditTableEntryEnabled()).thenReturn(false);

        ApiResponse response = searchService.deleteUserRecentSearchesByTimestamp(token, ts);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertTrue(((Map<?, ?>) response.getResult()).get("result").toString().contains("deleted successfully"));
        verify(cacheService).deleteCache(userId);
    }


    @Test
    void createUserRecentSearches_returnsErrorWhenSearchQueryIsNull() {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);

        ApiResponse response = searchService.createUserRecentSearches(null, token);

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

        ApiResponse response = searchService.createUserRecentSearches(searchQuery, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("One or more required fields (nlpSearchQuery, searchCategory, searchQuery) are missing or empty", response.getParams().getErrMsg());
    }

    @Test
    void readUserRecentSearches_returnsErrorWhenTokenIsInvalid() {
        String token = "invalidToken";

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(Constants.UNAUTHORIZED);

        ApiResponse response = searchService.readUserRecentSearches(token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(Constants.USER_ID_DOESNT_EXIST, response.getParams().getErrMsg());
    }

    @Test
    void readUserRecentSearches_returnsEmptyWhenNoSearchesExist() {
        String token = "validToken";
        String userId = "user123";

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);
        when(cbServerProperties.getRecentSearchesLimit()).thenReturn(10);
        when(cassandraOperation.getRecordsByOrder(anyString(), anyString(), anyMap(), anyInt(), any()))
                .thenReturn(Collections.emptyList());

        ApiResponse response = searchService.readUserRecentSearches(token);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals("User dont have any recent searches", response.getParams().getErrMsg());
    }

    @Test
    void deleteUserAllRecentSearches_returnsErrorWhenTokenIsInvalid() {
        String token = "invalidToken";

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(Constants.UNAUTHORIZED);

        ApiResponse response = searchService.deleteUserAllRecentSearches(token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(Constants.USER_ID_DOESNT_EXIST, response.getParams().getErrMsg());
    }

    @Test
    void deleteUserRecentSearchesByTimestamp_returnsErrorWhenTokenIsInvalid() {
        String token = "invalidToken";
        UUID timestamp = Uuids.timeBased();

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(Constants.UNAUTHORIZED);

        ApiResponse response = searchService.deleteUserRecentSearchesByTimestamp(token, timestamp);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(Constants.USER_ID_DOESNT_EXIST, response.getParams().getErrMsg());
    }

    @Test
    void deleteUserRecentSearchesByTimestamp_returnsErrorWhenDeleteFails() {
        String token = "validToken";
        String userId = "user123";
        UUID timestamp = Uuids.timeBased();
        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);
        Map<String, Object> existingRecord = new HashMap<>();
        existingRecord.put(Constants.USERID, userId);
        existingRecord.put(Constants.TIMESTAMP, timestamp);
        existingRecord.put(Constants.SEARCH_QUERY, "AI course");
        existingRecord.put(Constants.NLP_SEARCH_QUERY, "artificial intelligence");
        existingRecord.put(Constants.SEARCH_CATEGORY, Set.of("course"));

        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                anyString(), anyString(), anyMap(), isNull(), isNull()))
                .thenReturn(List.of(existingRecord));

        // make this stubbing lenient to avoid UnnecessaryStubbingException if not invoked
        lenient().when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap(), any()))
                .thenReturn(Map.of(Constants.RESPONSE, Constants.SUCCESS));

        // Mock delete fails
        when(cassandraOperation.deleteRecord(anyString(), anyString(), anyMap()))
                .thenReturn(Map.of(Constants.RESPONSE, Constants.FAILED, "errmsg", "Delete operation failed"));

        ApiResponse response = searchService.deleteUserRecentSearchesByTimestamp(token, timestamp);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("Delete operation failed", response.getParams().getErrMsg());
    }


    @Test
    void testCreateUserRecentSearches_CoversForLoop() {
        // Arrange
        String token = "valid-token";
        String userId = "user123";
        String actualQuery = "java course";

        // Mock token validation
        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);

        // Existing record that matches the incoming search query
        Map<String, Object> existingRecord = new HashMap<>();
        existingRecord.put(Constants.SEARCH_QUERY, actualQuery); // matches
        existingRecord.put(Constants.TIMESTAMP, 123456789L);
        existingRecord.put(Constants.SEARCH_CATEGORY, new HashSet<>(Arrays.asList("oldCategory")));

        List<Map<String, Object>> existingSearches = Collections.singletonList(existingRecord);

        // Mock Cassandra getRecordsByOrder to return a record
        when(cassandraOperation.getRecordsByOrder(
                anyString(), anyString(), anyMap(), isNull(), isNull()
        )).thenReturn(existingSearches);

        // Mock Cassandra insertRecord to return success
        ApiResponse insertResponse = ProjectUtil.createDefaultResponse(Constants.API_RECENT_SEARCH_CREATE);
        insertResponse.put(Constants.RESPONSE, Constants.SUCCESS);
        when(cassandraOperation.insertRecord(anyString(), anyString(), anyMap(), any()))
                .thenReturn(insertResponse);

        // Prepare JSON input with matching actualQuery
        com.fasterxml.jackson.databind.node.ObjectNode searchQueryJson = new ObjectMapper().createObjectNode();
        searchQueryJson.put(Constants.NLP_SEARCH_QUERY_KEY, "nlp text");
        searchQueryJson.put(Constants.SEARCH_CATEGORY_KEY, "newCategory");
        searchQueryJson.put(Constants.SEARCH_QUERY_KEY, actualQuery);

        // Act
        ApiResponse response = searchService.createUserRecentSearches(searchQueryJson, token);

        // Assert
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        verify(cassandraOperation, times(1)).deleteRecord(anyString(), anyString(), anyMap());
        verify(cacheService, times(1)).deleteCache(userId);
    }

    @Test
    void deleteUserRecentSearchesByTimestamp_deletesFailed() {
        String token = "validToken";
        String userId = "user123";
        UUID timestamp = Uuids.timeBased();

        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);

        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                anyString(), anyString(), anyMap(), isNull(), isNull())
        ).thenReturn(Collections.emptyList());

        ApiResponse response = searchService.deleteUserRecentSearchesByTimestamp(token, timestamp);

        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
        assertEquals("No recent search found for the given userid and timestamp", response.getParams().getErrMsg());
    }
}
