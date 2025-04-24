package com.igot.cb.search.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.elasticsearch.dto.SearchCriteria;
import com.igot.cb.search.service.SearchService;
import com.igot.cb.util.ApiResponse;
import com.igot.cb.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @PostMapping("/v1/recent/create")
    public ResponseEntity<ApiResponse> createUserRecentSearches(@RequestBody JsonNode searchQuery, @RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response = searchService.createUserRecentSearches(searchQuery,token);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @GetMapping("/v1/recent/read")
    public ResponseEntity<ApiResponse> readUserRecentSearches(@RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response = searchService.readUserRecentSearches(token);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @DeleteMapping("/v1/recent/delete")
    public ResponseEntity<ApiResponse> deleteUserRecentSearches(@RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response = searchService.deleteUserRecentSearches(token);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PostMapping("/v1/trending/create")
    public ResponseEntity<ApiResponse> createTrendingSearches(@RequestBody JsonNode searchQuery) {
        ApiResponse response = searchService.createUserTrendingSearches(searchQuery);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PostMapping("/v1/trending/read")
    public ResponseEntity<ApiResponse> readTrendingSearches(@RequestBody SearchCriteria searchCriteria) {
        ApiResponse response = searchService.readUserTrendingSearches(searchCriteria);
        return new ResponseEntity<>(response, response.getResponseCode());
    }
}
