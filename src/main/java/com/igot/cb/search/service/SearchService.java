package com.igot.cb.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.util.ApiResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public interface SearchService {
    ApiResponse createUserRecentSearches(JsonNode searchQuery, String token);

    ApiResponse readUserRecentSearches(String token);

    ApiResponse deleteUserAllRecentSearches(String token);

    ApiResponse deleteUserRecentSearchesByTimestamp(String token, UUID timestamp);
}
