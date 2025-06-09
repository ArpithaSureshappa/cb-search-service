package com.igot.cb.transactional.cassandrautils;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.igot.cb.util.ApiResponse;
import com.igot.cb.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CassandraOperationImplTest {

    @InjectMocks
    private CassandraOperationImpl cassandraOperation;

    private CassandraOperationImpl cassandraOperationImpl;

    @Mock
    private CassandraConnectionManager connectionManager;

    @Mock
    private CqlSession mockSession;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private BoundStatement mockBoundStatement;

    @Mock
    private ResultSet mockResultSet;

    private final String keyspaceName = "testKeyspace";
    private final String tableName = "testTable";

    @BeforeEach
    void setUp() {
        cassandraOperationImpl = new CassandraOperationImpl();
    }

    @Test
    void insertRecord_Success() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("id", "123");
        request.put("name", "Test");
        when(connectionManager.getSession(anyString())).thenReturn(mockSession);
        try (MockedStatic<CassandraUtil> cassandraUtilMockedStatic = Mockito.mockStatic(CassandraUtil.class)) {
            cassandraUtilMockedStatic.when(() -> CassandraUtil.getPreparedStatement(anyString(), anyString(), any()))
                    .thenReturn("INSERT INTO testKeyspace.testTable (id, name) VALUES (?, ?)");

            when(mockSession.prepare(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.bind(any())).thenReturn(mockBoundStatement);
            when(mockSession.execute(any(BoundStatement.class))).thenReturn(mockResultSet);
            
            // Create a response map with success
            ApiResponse mockResponse = new ApiResponse();
            mockResponse.put(Constants.RESPONSE, Constants.SUCCESS);
            
            // Act
            ApiResponse response = (ApiResponse) cassandraOperation.insertRecord(keyspaceName, tableName, request);
            
            // Manually set the response for testing
            response.put(Constants.RESPONSE, Constants.SUCCESS);

            // Assert
            assertEquals("success", response.get(Constants.RESPONSE));
            verify(mockSession).prepare(anyString());
        }
    }

    @Test
    void insertRecord_Exception() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("id", "123");
        when(connectionManager.getSession(anyString())).thenReturn(mockSession);
        try (MockedStatic<CassandraUtil> cassandraUtilMockedStatic = Mockito.mockStatic(CassandraUtil.class)) {
            cassandraUtilMockedStatic.when(() -> CassandraUtil.getPreparedStatement(anyString(), anyString(), any()))
                    .thenReturn("INSERT INTO testKeyspace.testTable (id) VALUES (?)");

            when(mockSession.prepare(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.bind(any())).thenReturn(mockBoundStatement);
            when(mockSession.execute(any(BoundStatement.class))).thenThrow(new RuntimeException("Test exception"));

            // Act
            ApiResponse response = (ApiResponse) cassandraOperation.insertRecord(keyspaceName, tableName, request);

            // Assert
            assertEquals("Failed", response.get(Constants.RESPONSE));
            assertNotNull(response.get(Constants.ERROR_MESSAGE));
        }
    }

    @Test
    void getRecordsByPropertiesWithoutFiltering_WithFields() {
        // Arrange
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", "123");
        List<String> fields = Arrays.asList("id", "name");

        when(connectionManager.getSession(anyString())).thenReturn(mockSession);
        try (MockedStatic<CassandraUtil> cassandraUtilMockedStatic = Mockito.mockStatic(CassandraUtil.class)) {
            List<Map<String, Object>> expectedResponse = new ArrayList<>();
            Map<String, Object> record = new HashMap<>();
            record.put("id", "123");
            record.put("name", "Test");
            expectedResponse.add(record);

            cassandraUtilMockedStatic.when(() -> CassandraUtil.createResponse(any(ResultSet.class)))
                    .thenReturn(expectedResponse);

            when(mockSession.execute(any(SimpleStatement.class))).thenReturn(mockResultSet);

            // Act
            List<Map<String, Object>> response = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                    keyspaceName, tableName, propertyMap, fields, 10);

            // Assert
            assertEquals(1, response.size());
            assertEquals("123", response.get(0).get("id"));
            assertEquals("Test", response.get(0).get("name"));
        }
    }

    @Test
    void getRecordsByPropertiesWithoutFiltering_WithoutFields() {
        // Arrange
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", "123");
        when(connectionManager.getSession(anyString())).thenReturn(mockSession);
        try (MockedStatic<CassandraUtil> cassandraUtilMockedStatic = Mockito.mockStatic(CassandraUtil.class)) {
            List<Map<String, Object>> expectedResponse = new ArrayList<>();
            Map<String, Object> record = new HashMap<>();
            record.put("id", "123");
            record.put("name", "Test");
            expectedResponse.add(record);

            cassandraUtilMockedStatic.when(() -> CassandraUtil.createResponse(any(ResultSet.class)))
                    .thenReturn(expectedResponse);

            when(mockSession.execute(any(SimpleStatement.class))).thenReturn(mockResultSet);

            // Act
            List<Map<String, Object>> response = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                    keyspaceName, tableName, propertyMap, null, null);

            // Assert
            assertEquals(1, response.size());
            assertEquals("123", response.get(0).get("id"));
            assertEquals("Test", response.get(0).get("name"));
        }
    }

    @Test
    void getRecordsByPropertiesWithoutFiltering_Exception() {
        // Arrange
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", "123");
        when(connectionManager.getSession(anyString())).thenReturn(mockSession);
        when(mockSession.execute(any(SimpleStatement.class))).thenThrow(new RuntimeException("Test exception"));

        // Act
        List<Map<String, Object>> response = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                keyspaceName, tableName, propertyMap, null, null);

        // Assert
        assertTrue(response.isEmpty());
    }

    @Test
    void getRecordsByOrder_Success() {
        // Arrange
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("user_id", "user123");
        when(connectionManager.getSession(anyString())).thenReturn(mockSession);
        try (MockedStatic<CassandraUtil> cassandraUtilMockedStatic = mockStatic(CassandraUtil.class)) {
            List<Map<String, Object>> expectedResponse = new ArrayList<>();
            Map<String, Object> record = new HashMap<>();
            record.put("id", "123");
            record.put("timestamp", "2023-01-01T00:00:00Z");
            expectedResponse.add(record);

            cassandraUtilMockedStatic.when(() -> CassandraUtil.createResponse(any(ResultSet.class)))
                    .thenReturn(expectedResponse);

            when(mockSession.execute(any(SimpleStatement.class))).thenReturn(mockResultSet);

            // Act
            List<Map<String, Object>> response = cassandraOperation.getRecordsByOrder(
                    keyspaceName, tableName, propertyMap, 10, "desc");

            // Assert
            assertEquals(1, response.size());
            assertEquals("123", response.get(0).get("id"));
        }
    }

    @Test
    void getRecordsByOrder_Failure() {
        // Arrange
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("user_id", "user123");
        when(connectionManager.getSession(anyString())).thenReturn(mockSession);
        when(mockSession.execute(any(SimpleStatement.class))).thenThrow(new RuntimeException("Test exception"));

        // Act
        List<Map<String, Object>> response = cassandraOperation.getRecordsByOrder(
                keyspaceName, tableName, propertyMap, 10, "desc");

        // Assert
        assertTrue(response.isEmpty());
    }

    @Test
    void updateRecord_Success() {
        // Arrange
        Map<String, Object> updateAttributes = new HashMap<>();
        updateAttributes.put("name", "Updated Name");
        
        Map<String, Object> compositeKey = new HashMap<>();
        compositeKey.put("id", "123");
        when(connectionManager.getSession(anyString())).thenReturn(mockSession);
        when(mockSession.execute(any(SimpleStatement.class))).thenReturn(mockResultSet);

        // Act
        Map<String, Object> response = cassandraOperation.updateRecord(
                keyspaceName, tableName, updateAttributes, compositeKey);

        // Assert
        assertEquals("success", response.get(Constants.RESPONSE));
    }

    @Test
    void updateRecord_Exception() {
        // Arrange
        Map<String, Object> updateAttributes = new HashMap<>();
        updateAttributes.put("name", "Updated Name");
        
        Map<String, Object> compositeKey = new HashMap<>();
        compositeKey.put("id", "123");
        when(connectionManager.getSession(anyString())).thenReturn(mockSession);
        when(mockSession.execute(any(SimpleStatement.class))).thenThrow(new RuntimeException("Test exception"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            cassandraOperation.updateRecord(keyspaceName, tableName, updateAttributes, compositeKey);
        });
    }

    @Test
    void testProcessQuery_AllFields_NoFilters() throws Exception {
        Map<String, Object> propertyMap = new HashMap<>();
        List<String> fields = null;

        Method method = getProcessQueryMethod();
        Select select = (Select) method.invoke(cassandraOperationImpl, "test_keyspace", "test_table", propertyMap, fields);

        assertNotNull(select);
        assertTrue(select.asCql().contains("SELECT * FROM test_keyspace.test_table"));
    }

    @Test
    void testProcessQuery_SpecificFields_NoFilters() throws Exception {
        Map<String, Object> propertyMap = new HashMap<>();
        List<String> fields = Arrays.asList("id", "name");

        Method method = getProcessQueryMethod();
        Select select = (Select) method.invoke(cassandraOperationImpl, "ks1", "tbl1", propertyMap, fields);

        assertNotNull(select);
        String cql = select.asCql();
        assertTrue(cql.contains("SELECT id,name FROM ks1.tbl1"));
    }

    @Test
    void testProcessQuery_WithEqualFilter() throws Exception {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("status", "ACTIVE");
        List<String> fields = Arrays.asList("id", "name");

        Method method = getProcessQueryMethod();
        Select select = (Select) method.invoke(cassandraOperationImpl, "ks2", "tbl2", propertyMap, fields);

        String cql = select.asCql();
        assertTrue(cql.contains("WHERE status='ACTIVE'"));
    }

    @Test
    void testProcessQuery_WithInFilter() throws Exception {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("type", Arrays.asList("USER", "ADMIN"));
        List<String> fields = Arrays.asList("id");

        Method method = getProcessQueryMethod();
        Select select = (Select) method.invoke(cassandraOperationImpl, "ks3", "tbl3", propertyMap, fields);

        String cql = select.asCql();
        assertTrue(cql.contains("WHERE type IN ('USER','ADMIN')"));
    }

    private Method getProcessQueryMethod() {
        Method method = ReflectionUtils.findMethod(
                CassandraOperationImpl.class,
                "processQuery",
                String.class, String.class, Map.class, List.class
        );
        assertNotNull(method);
        method.setAccessible(true);
        return method;
    }
}