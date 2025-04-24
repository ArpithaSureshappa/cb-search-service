package com.igot.cb.search.service;

import com.fasterxml.jackson.databind.JsonNode;

import com.igot.cb.elasticsearch.dto.SearchCriteria;
import com.igot.cb.util.ApiResponse;
import org.springframework.stereotype.Service;


@Service
public interface SearchService {
    ApiResponse createUserRecentSearches(JsonNode searchQuery, String token);

    ApiResponse readUserRecentSearches(String token);

    ApiResponse deleteUserRecentSearches(String token);

    ApiResponse createUserTrendingSearches(JsonNode searchQuery);

    ApiResponse readUserTrendingSearches(SearchCriteria searchCriteria);
}
