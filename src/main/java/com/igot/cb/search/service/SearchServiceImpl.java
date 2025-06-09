package com.igot.cb.search.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.authentication.util.AccessTokenValidator;
import com.igot.cb.util.*;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import com.igot.cb.util.redis.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
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
    @Override
    public ApiResponse createUserRecentSearches(JsonNode searchQuery, String token) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_RECENT_SEARCH_CREATE);
        String userId = accessTokenValidator.verifyUserToken(token);

        if (StringUtils.isBlank(userId) || Constants.UNAUTHORIZED.equalsIgnoreCase(userId)) {
            return errorResponse(response, HttpStatus.BAD_REQUEST, Constants.USER_ID_DOESNT_EXIST);
        }

        if (searchQuery == null || searchQuery.isEmpty()) {
            return errorResponse(response, HttpStatus.BAD_REQUEST, "search query is empty");
        }

        String nlpSearchQuery = searchQuery.path(Constants.NLP_SEARCH_QUERY_KEY).asText(null);
        String categoryNode = searchQuery.path(Constants.SEARCH_CATEGORY_KEY).asText(null);
        String actualQuery = searchQuery.hasNonNull(Constants.SEARCH_QUERY_KEY)
                ? searchQuery.get(Constants.SEARCH_QUERY_KEY).asText().toLowerCase().trim().replaceAll("\\s+", " ")
                : null;


        if (StringUtils.isBlank(nlpSearchQuery) || StringUtils.isBlank(actualQuery) || StringUtils.isBlank(categoryNode)) {
            return errorResponse(response, HttpStatus.BAD_REQUEST, "One or more required fields (nlpSearchQuery, searchCategory, searchQuery) are missing or empty");
        }

        Set<String> categorySet = new HashSet<>(Collections.singleton(categoryNode));

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put(Constants.USERID, userId);
        List<Map<String, Object>> existingSearches = cassandraOperation.getRecordsByOrder(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_USER_RECENT_SEARCH,
                queryMap,
                10,
                null
        );

        Optional<Map<String, Object>> matchedRecordOpt = existingSearches.stream()
                .filter(record -> actualQuery.equalsIgnoreCase((String) record.get(Constants.SEARCH_QUERY)))
                .findFirst();

        long currentTimestamp = System.currentTimeMillis();
        Map<String, Object> userSearchQuery = new HashMap<>();
        userSearchQuery.put(Constants.USERID, userId);
        userSearchQuery.put(Constants.TIMESTAMP, currentTimestamp);
        userSearchQuery.put(Constants.NLP_SEARCH_QUERY, nlpSearchQuery);
        userSearchQuery.put(Constants.SEARCH_CATEGORY, categorySet);
        userSearchQuery.put(Constants.SEARCH_QUERY, actualQuery);

        if (matchedRecordOpt.isPresent()) {
            Map<String, Object> matchedRecord = matchedRecordOpt.get();
            Map<String, Object> compositeKey = Map.of(
                    Constants.USERID, userId,
                    Constants.TIMESTAMP, matchedRecord.get(Constants.TIMESTAMP)
            );
            cassandraOperation.deleteRecord(Constants.KEYSPACE_SUNBIRD_COURSES, Constants.TABLE_USER_RECENT_SEARCH, compositeKey);
            Set<String> existingCategories = (Set<String>) matchedRecord.get(Constants.SEARCH_CATEGORY);
            if (existingCategories != null) {
                categorySet.addAll(existingCategories);
            }
            userSearchQuery.put(Constants.SEARCH_CATEGORY, categorySet);
        }

        ApiResponse dbResponse = (ApiResponse) cassandraOperation.insertRecord(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_USER_RECENT_SEARCH,
                userSearchQuery
        );

        if (Constants.SUCCESS.equals(dbResponse.get(Constants.RESPONSE))) {
            cacheService.deleteCache(userId);
            response.getParams().setErrMsg("search request saved successfully");
            response.getParams().setStatus(Constants.SUCCESS);
            response.setResponseCode(HttpStatus.OK);
        } else {
            response.getParams().setErrMsg("Failed to save search query.");
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
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
                List<Map<String, Object>> result= objectMapper.readValue(cachedJson, new TypeReference<List<Map<String, Object>>>() {
                });
                response.put("searchQueries", result);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            Map<String, Object> propertyMap = new HashMap<>();
            propertyMap.put(Constants.USERID, userId);
            List<Map<String, Object>> userSearchList = cassandraOperation.getRecordsByOrder(Constants.KEYSPACE_SUNBIRD_COURSES,
                    Constants.TABLE_USER_RECENT_SEARCH,
                    propertyMap,
                    10,
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
        Map<String, Object> deleteResponse = cassandraOperation.deleteRecord(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_USER_RECENT_SEARCH,
                propertyMap
        );
        if (!Constants.SUCCESS.equals(deleteResponse.get(Constants.RESPONSE))) {
            return errorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, deleteResponse.get("errmsg").toString());
        }else {
            cacheService.deleteCache(userId);
            response.setResponseCode(HttpStatus.OK);
            response.setResult(deleteResponse);
        }
        return response;
    }

    @Override
    public ApiResponse deleteUserRecentSearchesByTimestamp(String token, Long timestamp) {
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
        Map<String, Object> deleteResponse = cassandraOperation.deleteRecord(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_USER_RECENT_SEARCH,
                propertyMap
        );
        if (!Constants.SUCCESS.equals(deleteResponse.get(Constants.RESPONSE))) {
            return errorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, deleteResponse.get("errmsg").toString());
        }else {
            cacheService.deleteCache(userId);
            response.setResponseCode(HttpStatus.OK);
            response.setResult(deleteResponse);
        }
        return response;
    }
}
