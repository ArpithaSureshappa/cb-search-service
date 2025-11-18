package com.igot.cb.search.controller;


import com.igot.cb.search.service.SearchService;
import com.igot.cb.util.ApiResponse;
import com.igot.cb.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchController searchController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(searchController).build();
    }

    @Test
    void createUserRecentSearches() throws Exception {
        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        mockResponse.put("message", "Search created successfully");
        when(searchService.createUserRecentSearches(any(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/search/v1/recent/create")
                        .header(Constants.X_AUTH_TOKEN, "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.message").value("Search created successfully"));
    }

    @Test
    void readUserRecentSearches() throws Exception {
        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        mockResponse.put("message", "Searches retrieved successfully");
        when(searchService.readUserRecentSearches(anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/search/v1/recent/read")
                        .header(Constants.X_AUTH_TOKEN, "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.message").value("Searches retrieved successfully"));
    }

    @Test
    void deleteUserRecentSearches() throws Exception {
        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        mockResponse.put("message", "All searches deleted successfully");
        when(searchService.deleteUserAllRecentSearches(anyString())).thenReturn(mockResponse);

        mockMvc.perform(delete("/search/v1/recent/delete")
                        .header(Constants.X_AUTH_TOKEN, "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.message").value("All searches deleted successfully"));
    }

    @Test
    void deleteUserRecentSearchesByUniqueId() throws Exception {
        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        mockResponse.put("message", "Search deleted successfully");
        when(searchService.deleteUserRecentSearchesByTimestamp(anyString(), any())).thenReturn(mockResponse);

        UUID ts = UUID.randomUUID(); // use a valid UUID so Spring can bind the path variable
        mockMvc.perform(delete("/search/v1/recent/delete/timestamp/" + ts.toString())
                        .header(Constants.X_AUTH_TOKEN, "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.message").value("Search deleted successfully"));
    }
}
