package com.igot.cb.search.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.search.service.SearchService;
import com.igot.cb.util.Constants;

import org.igot.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
public class SearchController {

    private SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

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
        ApiResponse response = searchService.deleteUserAllRecentSearches(token);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @DeleteMapping("/v1/recent/delete/timestamp/{timestamp}")
    public ResponseEntity<ApiResponse> deleteUserRecentSearchesByUniqueId(@RequestHeader(Constants.X_AUTH_TOKEN) String token, @PathVariable Long timestamp) {
        ApiResponse response = searchService.deleteUserRecentSearchesByTimestamp(token,timestamp);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

}
