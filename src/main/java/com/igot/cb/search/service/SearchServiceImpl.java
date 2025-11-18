package com.igot.cb.search.service;


import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.authentication.util.AccessTokenValidator;
import com.igot.cb.util.*;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import com.igot.cb.util.redis.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AccessTokenValidator accessTokenValidator;

    @Autowired
    private CassandraOperation cassandraOperation;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CbServerProperties cbServerProperties;

    @Override
    public ApiResponse createUserRecentSearches(JsonNode searchQuery, String token) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_RECENT_SEARCH_CREATE);
        String userId = accessTokenValidator.verifyUserToken(token);
        Map<String, Object> errorMessage = validateInput(userId, searchQuery);

        if (errorMessage.containsKey(Constants.ERROR_MESSAGE)) {
            return errorResponse(response, HttpStatus.BAD_REQUEST, (String) errorMessage.get(Constants.ERROR_MESSAGE));
        }

        Set<String> categorySet = new HashSet<>(Collections.singleton(String.valueOf(errorMessage.get(Constants.SEARCH_CATEGORY))));

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put(Constants.USERID, userId);
        queryMap.put(Constants.QUERY_NORM, errorMessage.get(Constants.ACTUAL_QUERY));
        List<Map<String, Object>> existingSearches = cassandraOperation.getRecordsByOrder(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_SEARCH_INDEX_BY_USER,
                queryMap,
                null,
                null
        );

        for (Map<String, Object> recordMap : existingSearches) {
            Map<String, Object> compositeKey = Map.of(
                    Constants.USERID, userId,
                    Constants.TIMESTAMP, recordMap.get(Constants.TIMESTAMP)
            );

            cassandraOperation.deleteRecord(Constants.KEYSPACE_SUNBIRD_COURSES,
                    Constants.TABLE_USER_RECENT_SEARCH, compositeKey);
        }
        UUID currentTimeUuid = Uuids.timeBased();
        Map<String, Object> userQuery = new HashMap<>();
        userQuery.put(Constants.USERID, userId);
        userQuery.put(Constants.QUERY_NORM, errorMessage.get(Constants.ACTUAL_QUERY));
        userQuery.put(Constants.TIMESTAMP, currentTimeUuid);
        ApiResponse insertRecord = (ApiResponse) cassandraOperation.insertRecord(Constants.KEYSPACE_SUNBIRD_COURSES, Constants.TABLE_SEARCH_INDEX_BY_USER, userQuery, 604800);

        if (Constants.SUCCESS.equals(insertRecord.get(Constants.RESPONSE))) {
            Map<String, Object> userSearchQuery = new HashMap<>();
            userSearchQuery.put(Constants.USERID, userId);
            userSearchQuery.put(Constants.TIMESTAMP, currentTimeUuid);
            userSearchQuery.put(Constants.NLP_SEARCH_QUERY, errorMessage.get(Constants.NLP_SEARCH_QUERY));
            userSearchQuery.put(Constants.SEARCH_CATEGORY, categorySet);
            userSearchQuery.put(Constants.SEARCH_QUERY, errorMessage.get(Constants.ACTUAL_QUERY));
            cassandraOperation.insertRecord(
                    Constants.KEYSPACE_SUNBIRD_COURSES,
                    Constants.TABLE_USER_RECENT_SEARCH,
                    userSearchQuery,
                    null
            );
            cacheService.deleteCache(userId);

            if (cbServerProperties.getIsAuditTableEntryEnabled()) {
                userSearchQuery.put(Constants.OPERATION, Constants.ADD);
                cassandraOperation.insertRecord(
                        Constants.KEYSPACE_SUNBIRD_COURSES,
                        Constants.TABLE_USER_RECENT_SEARCH_AUDIT,
                        userSearchQuery, null
                );
            }
            response.getParams().setErrMsg("search request saved successfully");
            response.getParams().setStatus(Constants.SUCCESS);
            response.setResponseCode(HttpStatus.OK);
        } else {
            response.getParams().setErrMsg("Failed to save search query.");
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    private Map<String,Object> validateInput(String userId, JsonNode searchQuery) {
        Map<String, Object> validationResult = new HashMap<>();
        if (StringUtils.isBlank(userId) || Constants.UNAUTHORIZED.equalsIgnoreCase(userId)) {
            validationResult.put(Constants.ERROR_MESSAGE, Constants.USER_ID_DOESNT_EXIST);
            return validationResult;
        }

        if (searchQuery == null || searchQuery.isEmpty()) {
            validationResult.put(Constants.ERROR_MESSAGE, "search query is empty");
            return validationResult;
        }

        String nlp = searchQuery.path(Constants.NLP_SEARCH_QUERY_KEY).asText(null);
        String category = searchQuery.path(Constants.SEARCH_CATEGORY_KEY).asText(null);
        String actualQuery = searchQuery.hasNonNull(Constants.SEARCH_QUERY_KEY)
                ? searchQuery.get(Constants.SEARCH_QUERY_KEY).asText().toLowerCase().trim().replaceAll("\\s+", " ")
                : null;

        if (StringUtils.isAnyBlank(nlp, actualQuery, category)) {
            validationResult.put(Constants.ERROR_MESSAGE, "One or more required fields (nlpSearchQuery, searchCategory, searchQuery) are missing or empty");
            return validationResult;
        }
        validationResult.put(Constants.ACTUAL_QUERY,actualQuery);
        validationResult.put(Constants.NLP_SEARCH_QUERY,nlp);
        validationResult.put(Constants.SEARCH_CATEGORY,category);
        return validationResult;
    }


    private ApiResponse errorResponse(ApiResponse response, HttpStatus status, String errorMsg) {
        response.getParams().setErrMsg(errorMsg);
        response.getParams().setStatus(Constants.FAILED);
        response.setResponseCode(status);
        return response;
    }

    @Override
    public ApiResponse readUserRecentSearches(String token) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_RECENT_SEARCH_READ);
        String userId = accessTokenValidator.verifyUserToken(token);
        if (StringUtils.isBlank(userId) || userId.equalsIgnoreCase(Constants.UNAUTHORIZED)) {
            response.getParams().setErrMsg(Constants.USER_ID_DOESNT_EXIST);
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
        String cachedJson = cacheService.getCache(userId);
        if (StringUtils.isNotEmpty(cachedJson)) {
            log.info("SearchServiceImpl::read:Record coming from redis cache");
            try {
                List<Map<String, Object>> result = objectMapper.readValue(cachedJson, new TypeReference<List<Map<String, Object>>>() {
                });
                response.put(Constants.SEARCH_QUERIES, result);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            Map<String, Object> propertyMap = new HashMap<>();
            propertyMap.put(Constants.USERID, userId);
            List<Map<String, Object>> userSearchList = cassandraOperation.getRecordsByOrder(Constants.KEYSPACE_SUNBIRD_COURSES,
                    Constants.TABLE_USER_RECENT_SEARCH,
                    propertyMap,
                    cbServerProperties.getRecentSearchesLimit(),
                    Constants.ORDER_DESC
            );

            if (!userSearchList.isEmpty()) {
                response.put("searchQueries", userSearchList);
                cacheService.putCache(userId, userSearchList);
            } else {
                response.getParams().setErrMsg("User dont have any recent searches");
                response.getParams().setStatus(Constants.SUCCESS);
                response.setResponseCode(HttpStatus.OK);
                return response;
            }
        }

        response.setResponseCode(HttpStatus.OK);
        response.setResult(response.getResult());
        return response;
    }

    @Override
    public ApiResponse deleteUserAllRecentSearches(String token) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_RECENT_SEARCH_DELETE);
        String userId = accessTokenValidator.verifyUserToken(token);
        if (StringUtils.isBlank(userId) || userId.equalsIgnoreCase(Constants.UNAUTHORIZED)) {
            response.getParams().setErrMsg(Constants.USER_ID_DOESNT_EXIST);
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.USERID, userId);

        List<Map<String, Object>> records = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_USER_RECENT_SEARCH,
                propertyMap,
                null,
                null);

        for (Map<String, Object> recordMap : records) {
            Map<String, Object> primaryKey = new HashMap<>();
            primaryKey.put(Constants.USERID, userId);
            primaryKey.put(Constants.TIMESTAMP, recordMap.get(Constants.TIMESTAMP));
            cassandraOperation.deleteRecord(Constants.KEYSPACE_SUNBIRD_COURSES, Constants.TABLE_USER_RECENT_SEARCH, primaryKey);
            if(cbServerProperties.getIsAuditTableEntryEnabled()) {
                recordMap.put(Constants.OPERATION, Constants.REMOVE);
                cassandraOperation.insertRecord(
                        Constants.KEYSPACE_SUNBIRD_COURSES,
                        Constants.TABLE_USER_RECENT_SEARCH_AUDIT,
                        recordMap, null
                );
            }
        }
        Map<String, Object> deleteResponse = new HashMap<>();
        deleteResponse.put("result", "All recent searches deleted successfully");
        cacheService.deleteCache(userId);
        response.setResponseCode(HttpStatus.OK);
        response.setResult(deleteResponse);
        return response;
    }

    @Override
    public ApiResponse deleteUserRecentSearchesByTimestamp(String token, UUID timestamp) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_RECENT_SEARCH_DELETE);
        String userId = accessTokenValidator.verifyUserToken(token);
        if (StringUtils.isBlank(userId) || userId.equalsIgnoreCase(Constants.UNAUTHORIZED)) {
            response.getParams().setErrMsg(Constants.USER_ID_DOESNT_EXIST);
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.USERID, userId);
        propertyMap.put(Constants.TIMESTAMP, timestamp);
        List<Map<String, Object>> records = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_USER_RECENT_SEARCH,
                propertyMap,
                null,
                null);

        if (!records.isEmpty()) {
            Map<String, Object> recordMap = records.get(0);
            Map<String, Object> primaryKey = new HashMap<>();
            primaryKey.put("user_id", userId);
            primaryKey.put("timestamp", recordMap.get(Constants.TIMESTAMP));
            Map<String, Object> deleteResult = cassandraOperation.deleteRecord(Constants.KEYSPACE_SUNBIRD_COURSES, Constants.TABLE_USER_RECENT_SEARCH, propertyMap);
            if (!Constants.SUCCESS.equalsIgnoreCase(String.valueOf(deleteResult.get(Constants.RESPONSE)))) {
                return errorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, String.valueOf(deleteResult.get("errmsg")));
            }
            if(cbServerProperties.getIsAuditTableEntryEnabled()) {
                recordMap.put("op", "REMOVE");
                cassandraOperation.insertRecord(
                        Constants.KEYSPACE_SUNBIRD_COURSES,
                        Constants.TABLE_USER_RECENT_SEARCH_AUDIT,
                        recordMap, null
                );
            }
            cacheService.deleteCache(userId);
        } else {
            log.error("SearchServiceImpl::deleteUserRecentSearchesByTimestamp: No recent search found for userId: {} and timestamp: {}", userId, timestamp);
            return errorResponse(response, HttpStatus.NOT_FOUND, "No recent search found for the given userid and timestamp");
        }
        Map<String, Object> deleteResponse = new HashMap<>();
        deleteResponse.put("result", "Recent search deleted successfully");

        response.setResponseCode(HttpStatus.OK);
        response.setResult(deleteResponse);

        return response;
    }
}
