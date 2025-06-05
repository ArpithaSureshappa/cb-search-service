package com.igot.cb.transactional.cassandrautils;


import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.schema.ClusteringOrder;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.delete.Delete;
import com.datastax.oss.driver.api.querybuilder.delete.DeleteSelection;
import com.datastax.oss.driver.api.querybuilder.relation.Relation;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.datastax.oss.driver.api.querybuilder.term.Term;
import com.datastax.oss.driver.api.querybuilder.update.Assignment;
import com.datastax.oss.driver.api.querybuilder.update.Update;
import com.datastax.oss.driver.api.querybuilder.update.UpdateStart;
import com.datastax.oss.driver.api.querybuilder.update.UpdateWithAssignments;
import com.igot.cb.util.ApiResponse;
import com.igot.cb.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Component
public class CassandraOperationImpl implements CassandraOperation {

    @Autowired
    CassandraConnectionManager connectionManager;

    private Select processQuery(String keyspaceName, String tableName, Map<String, Object> propertyMap,
                                List<String> fields) {
        Select select;
        if (CollectionUtils.isNotEmpty(fields)) {
            select = QueryBuilder.selectFrom(keyspaceName, tableName).columns(fields);
        } else {
            select = QueryBuilder.selectFrom(keyspaceName, tableName).all();
        }
        if (MapUtils.isEmpty(propertyMap)) {
            return select; // Build and return the query
        }
        for (Map.Entry<String, Object> entry : propertyMap.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof List) {
                List<?> valueList = (List<?>) value;
                if (CollectionUtils.isNotEmpty(valueList)) {
                    List<Term> terms = valueList.stream()
                            .map(QueryBuilder::literal)
                            .collect(Collectors.toList());
                    select = select.whereColumn(columnName).in(terms);
                }
            } else {
                select = select.whereColumn(columnName).isEqualTo(QueryBuilder.literal(value));
            }
        }
        return select;
    }


    private Select processQueryWithoutFiltering(String keyspaceName, String tableName, Map<String, Object> propertyMap,
                                                List<String> fields) {
        Select selectQuery;
        if (CollectionUtils.isNotEmpty(fields)) {
            selectQuery = QueryBuilder.selectFrom(keyspaceName, tableName).columns(fields);
        } else {
            selectQuery = QueryBuilder.selectFrom(keyspaceName, tableName).all();
        }
        if (MapUtils.isNotEmpty(propertyMap)) {
            for (Map.Entry<String, Object> entry : propertyMap.entrySet()) {
                String columnName = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof List) {
                    List<?> valueList = (List<?>) value;
                    if (CollectionUtils.isNotEmpty(valueList)) {
                        List<Term> terms = valueList.stream()
                                .map(QueryBuilder::literal)
                                .collect(Collectors.toList());
                        selectQuery = selectQuery.whereColumn(columnName).in(terms);
                    }
                } else {
                    selectQuery = selectQuery.whereColumn(columnName).isEqualTo(QueryBuilder.literal(value));
                }
            }
        }
        return selectQuery;
    }

    @Override
    public List<Map<String, Object>> getRecordsByPropertiesByKey(String keyspaceName,
                                                                 String tableName, Map<String, Object> propertyMap, List<String> fields, String key) {
        Select selectQuery = null;
        List<Map<String, Object>> response = new ArrayList<>();
        try {
            selectQuery = processQuery(keyspaceName, tableName, propertyMap, fields);
            CqlSession session = connectionManager.getSession(keyspaceName);
            ResultSet results = session.execute(selectQuery.build());
            response = CassandraUtil.createResponse(results);
            log.info(response.toString());

        } catch (Exception e) {
            log.error(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
        }
        return response;
    }

    @Override
    public Object insertRecord(String keyspaceName, String tableName, Map<String, Object> request) {
        ApiResponse response = new ApiResponse();
        try {
            String query = CassandraUtil.getPreparedStatement(keyspaceName, tableName, request);
            CqlSession session = connectionManager.getSession(keyspaceName);
            PreparedStatement statement = session.prepare(query);
            BoundStatement boundStatement = statement.bind(request.values().toArray());
            session.execute(boundStatement);
            response.put(Constants.RESPONSE, Constants.SUCCESS);
        } catch (Exception e) {
            String errMsg = String.format("Exception occurred while inserting record to %s %s", tableName, e.getMessage());
            log.error("Error inserting record into {}: {}", tableName, e.getMessage());
            response.put(Constants.RESPONSE, Constants.FAILED);
            response.put(Constants.ERROR_MESSAGE, errMsg);
        }
        return response;
    }

    @Override
    public List<Map<String, Object>> getRecordsByPropertiesWithoutFiltering(String keyspaceName, String tableName, Map<String, Object> propertyMap, List<String> fields, Integer limit) {

        List<Map<String, Object>> response = new ArrayList<>();
        try {
            Select selectQuery = null;
            selectQuery = processQuery(keyspaceName, tableName, propertyMap, fields);

            if (limit != null) selectQuery = selectQuery.limit(limit);
            String queryString = selectQuery.toString();
            SimpleStatement statement = SimpleStatement.newInstance(queryString);
            ResultSet results = connectionManager.getSession(keyspaceName).execute(statement);
            response = CassandraUtil.createResponse(results);

        } catch (Exception e) {
            log.error("Error fetching records from {}: {}", tableName, e.getMessage());
        }
        return response;
    }

    @Override
    public Map<String, Object> updateRecord(String keyspaceName, String tableName, Map<String, Object> updateAttributes,
                                            Map<String, Object> compositeKey) {
        Map<String, Object> response = new HashMap<>();
        CqlSession session = null;
        try {
            session = connectionManager.getSession(keyspaceName);
            UpdateStart updateStart = QueryBuilder.update(keyspaceName, tableName);
            UpdateWithAssignments updateWithAssignments = updateStart.set(
                    updateAttributes.entrySet().stream()
                            .map(entry -> Assignment.setColumn(entry.getKey(), QueryBuilder.literal(entry.getValue())))
                            .toArray(Assignment[]::new)
            );
            Update update = updateWithAssignments.where(
                    compositeKey.entrySet().stream()
                            .map(entry -> Relation.column(entry.getKey()).isEqualTo(QueryBuilder.literal(entry.getValue())))
                            .toArray(Relation[]::new)
            );
            session.execute(update.build());
            response.put(Constants.RESPONSE, Constants.SUCCESS);
        } catch (Exception e) {
            String errMsg = String.format("Exception occurred while updating record to %s %s", tableName, e.getMessage());
            log.error(errMsg);
            response.put(Constants.RESPONSE, Constants.FAILED);
            response.put(Constants.ERROR_MESSAGE, errMsg);
            throw e;
        }
        return response;
    }

    public static String getUpdateQueryStatement(
            String keyspaceName, String tableName, Map<String, Object> map) {
        StringBuilder query =
                new StringBuilder(
                        Constants.UPDATE + keyspaceName + Constants.DOT + tableName + Constants.SET);
        Set<String> key = new HashSet<>(map.keySet());
        key.remove(Constants.ID);
        query.append(String.join(" = ? ,", key));
        query.append(
                Constants.EQUAL_WITH_QUE_MARK + Constants.WHERE_ID + Constants.EQUAL_WITH_QUE_MARK);
        return query.toString();
    }

    public List<Map<String, Object>> getRecordsByOrder(String keyspaceName, String tableName, Map<String, Object> propertyMap, Integer limit, String orderDirection) {
        List<Map<String, Object>> response = new ArrayList<>();
        try {
            Select selectQuery = null;
            selectQuery = processQuery(keyspaceName, tableName, propertyMap, null);
            CqlSession session = connectionManager.getSession(keyspaceName);
            if (propertyMap.containsKey("user_id")) {
                ClusteringOrder order = ClusteringOrder.DESC;
                if ("asc".equalsIgnoreCase(orderDirection)) {
                    order = ClusteringOrder.ASC;
                }
                selectQuery = selectQuery.orderBy("timestamp", order); // or Sort.asc("unique_id")
            }
            if (limit != null) selectQuery = selectQuery.limit(limit);
            String queryString = selectQuery.toString();
            SimpleStatement statement = SimpleStatement.newInstance(queryString);
            ResultSet results = session.execute(statement);
            response = CassandraUtil.createResponse(results);
        } catch (Exception e) {
            log.error("Exception occurred while fetching from " + tableName + ": " + e.getMessage(), e);
        }
        return response;
    }

    @Override
    public Map<String,Object> deleteRecord(String keyspaceName, String tableName, Map<String, Object> propertyMap) {
        Map<String, Object> response = new HashMap<>();
        DeleteSelection deleteSelection = null;
        try {
            deleteSelection = QueryBuilder.deleteFrom(keyspaceName, tableName);
            Delete delete = deleteSelection.where(
                    propertyMap.entrySet().stream()
                            .map(entry -> Relation.column(entry.getKey()).isEqualTo(QueryBuilder.literal(entry.getValue())))
                            .toArray(Relation[]::new)
            );
            SimpleStatement statement = delete.build();
            connectionManager.getSession(keyspaceName).execute(statement);
            response.put(Constants.RESPONSE, Constants.SUCCESS);
        } catch (Exception e) {
            log.error("Exception occurred while deleting from " + tableName + ": " + e.getMessage(), e);
            response.put(Constants.RESPONSE, Constants.FAILED);
            response.put(Constants.ERROR_MESSAGE, e.getMessage());
        }
       return response;
    }
}
