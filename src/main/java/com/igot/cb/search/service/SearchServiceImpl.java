package com.igot.cb.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.authentication.util.AccessTokenValidator;
import com.igot.cb.elasticsearch.dto.SearchCriteria;
import com.igot.cb.elasticsearch.dto.SearchResult;
import com.igot.cb.util.ApiResponse;
import com.igot.cb.util.CbServerProperties;
import com.igot.cb.util.Constants;
import com.igot.cb.util.ProjectUtil;
import com.igot.cb.elasticsearch.service.EsUtilService;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AccessTokenValidator accessTokenValidator;

    @Autowired
    private CassandraOperation cassandraOperation;

    @Autowired
    private EsUtilService esUtilService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CbServerProperties cbServerProperties;

    @Override
    public ApiResponse createUserRecentSearches(JsonNode searchQuery, String token) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_RECENT_SEARCH_CREATE);
        String userId = accessTokenValidator.verifyUserToken(token);
        if (StringUtils.isBlank(userId) || userId.equalsIgnoreCase(Constants.UNAUTHORIZED)) {
            response.getParams().setErrMsg(Constants.USER_ID_DOESNT_EXIST);
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }

        if (searchQuery == null || searchQuery.isEmpty()) {
            response.getParams().setErrMsg("search query is empty");
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.USERID, userId);

        List<Map<String, Object>> existingSearches = cassandraOperation.getRecordsByOrder(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_USER_RECENT_SEARCH,
                propertyMap,
                10,
                null
        );
        String newSearchQuery = searchQuery.get(Constants.SEARCH_QUERY).asText();
        boolean isDuplicate = existingSearches.stream()
                .anyMatch(record -> newSearchQuery.equalsIgnoreCase((String) record.get("search_query")));
        if (!isDuplicate) {
            Map<String, Object> userSearchQuery = new HashMap<>();
            userSearchQuery.put(Constants.USERID, userId);
            userSearchQuery.put(Constants.UNIQUE_ID, System.currentTimeMillis());
            userSearchQuery.put(Constants.SEARCH_QUERY_KEY, searchQuery.get(Constants.SEARCH_QUERY).asText());
            ApiResponse insertResponse = (ApiResponse) cassandraOperation.insertRecord(
                    Constants.KEYSPACE_SUNBIRD_COURSES,
                    Constants.TABLE_USER_RECENT_SEARCH,
                    userSearchQuery
            );

            if (Constants.SUCCESS.equals(insertResponse.get(Constants.RESPONSE))) {
                response.getParams().setErrMsg("search request saved successfully");
                response.getParams().setStatus(Constants.SUCCESS);
                response.setResponseCode(HttpStatus.OK);
                return response;
            } else {
                response.getParams().setErrMsg("Failed to save search query.");
                response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return response;
            }
        } else {
            response.getParams().setErrMsg("search request saved successfully");
            response.getParams().setStatus(Constants.SUCCESS);
            response.setResponseCode(HttpStatus.OK);
            return response;
        }
    }

    @Override
    public ApiResponse readUserRecentSearches(String token) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_RECENT_SEARCH_READ);
        String userId = accessTokenValidator.verifyUserToken(token);
        if (StringUtils.isBlank(userId)) {
            response.getParams().setErrMsg(Constants.USER_ID_DOESNT_EXIST);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
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
        } else {
            response.getParams().setErrMsg("User dont have any recent searches");
            response.getParams().setStatus(Constants.SUCCESS);
            response.setResponseCode(HttpStatus.OK);
            return response;
        }
        response.setResponseCode(HttpStatus.OK);
        response.setResult(response.getResult());
        return response;
    }

    @Override
    public ApiResponse deleteUserRecentSearches(String token) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_RECENT_SEARCH_DELETE);
        String userId = accessTokenValidator.verifyUserToken(token);
        if (StringUtils.isBlank(userId)) {
            response.getParams().setErrMsg(Constants.USER_ID_DOESNT_EXIST);
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
        response.setResponseCode(HttpStatus.OK);
        response.setResult(deleteResponse);
        return response;
    }


    @Override
    public ApiResponse createUserTrendingSearches(JsonNode searchQuery) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_TRENDING_SEARCH_CREATE);
        String query = searchQuery.get(Constants.SEARCH_QUERY).asText();
        String id = query.trim().toLowerCase().replaceAll("\\s+", " ");
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.QUERY_ID, id);

        List<Map<String, Object>> trendingRecord = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_TRENDING_SEARCH, propertyMap, null, null);
        if (trendingRecord.isEmpty()) {
            Map<String, Object> trendingSearch = new HashMap<>();
            trendingSearch.put(Constants.QUERY_ID, id);
            trendingSearch.put("query", searchQuery.get(Constants.SEARCH_QUERY).asText());
            trendingSearch.put(Constants.SEARCH_COUNT, 1);
            trendingSearch.put(Constants.LAST_SEARCHED, Instant.now());
            cassandraOperation.insertRecord(Constants.KEYSPACE_SUNBIRD_COURSES,
                    Constants.TABLE_TRENDING_SEARCH, trendingSearch);
            trendingSearch.put(Constants.LAST_SEARCHED, Instant.now().toString());
            trendingSearch.put(Constants.SEARCH_COUNT, trendingSearch.get(Constants.SEARCH_COUNT).toString());
            esUtilService.addDocument(Constants.TRENDING_SEARCHES_INDEX_NAME, Constants.INDEX_TYPE, id, trendingSearch, cbServerProperties.getElasticSearchJsonPath());
        } else {
            for (Map<String, Object> record : trendingRecord) {
                int searchCount = (int) record.get(Constants.SEARCH_COUNT);
                searchCount++;
                record.put(Constants.SEARCH_COUNT, searchCount);
                record.put(Constants.LAST_SEARCHED, Instant.now());
                cassandraOperation.insertRecord(Constants.KEYSPACE_SUNBIRD_COURSES,
                        Constants.TABLE_TRENDING_SEARCH, record);
                record.put(Constants.LAST_SEARCHED, Instant.now().toString());
                record.put(Constants.SEARCH_COUNT, record.get(Constants.SEARCH_COUNT).toString());
                esUtilService.addDocument(Constants.TRENDING_SEARCHES_INDEX_NAME, Constants.INDEX_TYPE, id, record, cbServerProperties.getElasticSearchJsonPath());
            }
        }
        return response;
    }

    @Override
    public ApiResponse readUserTrendingSearches(SearchCriteria searchCriteria) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_TRENDING_SEARCH_READ);
        try {
            SearchResult searchResult =
                    esUtilService.searchDocuments(Constants.TRENDING_SEARCHES_INDEX_NAME, searchCriteria);
            Map<String, Object> jsonMap =
                    objectMapper.convertValue(searchResult, new TypeReference<Map<String, Object>>() {
                    });
            response.setResult(jsonMap);
            response.setResponseCode(HttpStatus.OK);
        } catch (Exception e) {
            response.getParams().setErrMsg(e.getMessage());
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}
