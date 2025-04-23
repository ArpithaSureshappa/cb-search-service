package com.igot.cb.elasticsearch.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.elasticsearch.dto.SearchCriteria;
import com.igot.cb.elasticsearch.dto.SearchResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public interface EsUtilService {
  String addDocument(String esIndexName, String type, String id, Map<String, Object> document, String JsonFilePath);

  String updateDocument(String index, String indexType, String entityId, Map<String, Object> document, String JsonFilePath);

  void deleteDocument(String documentId, String esIndexName);

  void deleteDocumentsByCriteria(String esIndexName, Query query);

  SearchResult searchDocuments(String esIndexName, SearchCriteria searchCriteria) throws Exception;

  boolean isIndexPresent(String indexName);

  BulkResponse saveAll(String esIndexName, List<JsonNode> entities) throws IOException;

}
