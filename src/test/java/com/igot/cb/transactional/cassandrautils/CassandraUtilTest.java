package com.igot.cb.transactional.cassandrautils;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class CassandraUtilTest {

    private ResultSet mockResultSet;
    private Row mockRow;
    private ColumnDefinitions mockColumnDefinitions;
    private CassandraPropertyReader mockReader;

    private ColumnDefinition mockColumnDefinition;

    private CassandraPropertyReader mockPropertyReader;

    @BeforeEach
    void setUp() {
        mockResultSet = mock(ResultSet.class);
        mockRow = mock(Row.class);
        mockColumnDefinitions = mock(ColumnDefinitions.class);
        mockColumnDefinition = mock(ColumnDefinition.class);
        mockReader = mock(CassandraPropertyReader.class);
        mockPropertyReader = mock(CassandraPropertyReader.class);
    }

    @Test
    void testGetPreparedStatement() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", 1);
        data.put("name", "Mahesh");

        String actual = CassandraUtil.getPreparedStatement("test_keyspace", "test_table", data);
        String expected = "INSERT INTO test_keyspace.test_table(id,name) VALUES (?,?);";
        assertEquals(expected, actual);
    }

    @Test
    void testCreateResponseList() {
        when(mockResultSet.getColumnDefinitions()).thenReturn(mockColumnDefinitions);

        try (MockedStatic<CassandraPropertyReader> staticMock = mockStatic(CassandraPropertyReader.class)) {
            staticMock.when(CassandraPropertyReader::getInstance).thenReturn(mockReader);
            when(mockReader.readProperty("id")).thenReturn("id");

            when(mockResultSet.iterator()).thenReturn(List.of(mockRow).iterator());
            when(mockRow.getObject("id")).thenReturn("123");

            List<Map<String, Object>> result = CassandraUtil.createResponse(mockResultSet);
            assertEquals(1, result.size());
        }
    }

    @Test
    void testCreateResponseMap() {
        when(mockResultSet.getColumnDefinitions()).thenReturn(mockColumnDefinitions);

        try (MockedStatic<CassandraPropertyReader> staticMock = mockStatic(CassandraPropertyReader.class)) {
            staticMock.when(CassandraPropertyReader::getInstance).thenReturn(mockReader);
            when(mockReader.readProperty("id")).thenReturn("id");

            when(mockResultSet.iterator()).thenReturn(List.of(mockRow).iterator());
            when(mockRow.getObject("id")).thenReturn("123");

            Map<String, Object> result = CassandraUtil.createResponse(mockResultSet, "id");
            assertEquals(1, result.size());
        }
    }


    @Test
    void fetchColumnsMapping() {
        // Arrange
        when(mockResultSet.getColumnDefinitions()).thenReturn(mockColumnDefinitions);

        List<ColumnDefinition> columnDefinitions = new ArrayList<>();
        columnDefinitions.add(mockColumnDefinition);

        when(mockColumnDefinition.getName()).thenReturn(CqlIdentifier.fromCql("id"));

        try (MockedStatic<CassandraPropertyReader> propertyReaderMockedStatic = Mockito.mockStatic(CassandraPropertyReader.class)) {
            propertyReaderMockedStatic.when(CassandraPropertyReader::getInstance).thenReturn(mockPropertyReader);
            // Make sure the property reader returns a non-null value
            lenient().when(mockPropertyReader.readProperty("id")).thenReturn("userId");

            // Force the forEach to execute by mocking the behavior
            doAnswer(invocation -> {
                java.util.function.Consumer<ColumnDefinition> consumer = invocation.getArgument(0);
                for (ColumnDefinition def : columnDefinitions) {
                    consumer.accept(def);
                }
                return null;
            }).when(mockColumnDefinitions).forEach(any());

            // Act
            Map<String, String> columnsMapping = CassandraUtil.fetchColumnsMapping(mockResultSet);

            // Assert - verify the map contains the expected entry
            assertNotNull(columnsMapping);
            assertEquals(1, columnsMapping.size());
        }
    }

    @Test
    void testPrivateConstructor() throws Exception {
        var constructor = CassandraUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        CassandraUtil instance = constructor.newInstance();
        assertNotNull(instance);
    }
}