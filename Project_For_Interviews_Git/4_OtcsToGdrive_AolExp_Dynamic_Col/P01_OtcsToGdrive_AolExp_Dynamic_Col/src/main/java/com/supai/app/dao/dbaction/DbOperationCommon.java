package com.supai.app.dao.dbaction;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.DbTable;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbOperationCommon {
	private final DataSource dataSource;
	private final JsonObj jsonObj;
	private final DbTable dbTable;
	private final DbConnection dbConnection;
	

	public List<String> getAllColumnNamesEvenIfEmpty() {
		List<String> columnNames = new ArrayList<>();

		try (Connection conn = dbConnection.getConnection()) {
			DatabaseMetaData metaData = conn.getMetaData();
			try (ResultSet rs = metaData.getColumns(null, null, dbTable.getTableName().toUpperCase(), null)) {
				while (rs.next()) {
					columnNames.add(rs.getString("COLUMN_NAME").toLowerCase());
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return columnNames;
	}


	public void isColumnCorrect(Object columnSource) {
	    Set<String> columnsToMatch = getKeysFromInput(columnSource);
	    checkColumnsAgainstTable(columnsToMatch);
	}

	private Set<String> getKeysFromInput(Object columnSource) {// parameter can be either List<String> or JsonNode
	    Set<String> inputKeys = new HashSet<>();

	    if (columnSource instanceof List<?> list && !list.isEmpty()) {
	        Object first = list.get(0);

	        if (first instanceof JsonNode jsonNode) {
	            Map<String, Object> map = jsonObj.convertSimpleJsonNodeToMap(jsonNode);
	            inputKeys = map.keySet().stream()
	                    .map(String::toLowerCase)
	                    .collect(Collectors.toSet());
	        } else if (first instanceof String) {
	            inputKeys = list.stream()
	                    .map(Object::toString)
	                    .map(String::toLowerCase)
	                    .collect(Collectors.toSet());
	        } else {
	            log.error("X Unsupported list element type: {}", first.getClass().getName());
	            throw new IllegalArgumentException("Unsupported list element type: " + first.getClass().getName());
	        }
	    } else {
	        log.warn("Provided columnSource is empty or not a valid list.");
	    }

	    return inputKeys;
	}

	private void checkColumnsAgainstTable(Set<String> inputKeys) {
	    Set<String> tableColumns = new HashSet<>();

	    try (Connection conn = dbConnection.getConnection();
	         ResultSet rs = conn.getMetaData().getColumns(null, null, dbTable.getTableName().toUpperCase(), null)) {

	        while (rs.next()) {
	            String columnName = rs.getString("COLUMN_NAME");
	            tableColumns.add(columnName.toLowerCase());
	        }

	        Set<String> missingInTable = new HashSet<>(inputKeys);
	        missingInTable.removeAll(tableColumns);

	        if (missingInTable.isEmpty()) {
	            log.info("All keys exist in table '{}'.", dbTable.getTableName());
	        } else {
	            log.error("Missmatched Key From YML File and  table ['{}'] are [{}]", dbTable.getTableName(), missingInTable);
	            log.info("Available columns in table '{}': {}", dbTable.getTableName(), tableColumns);
	            throw new RuntimeException("YML Category key and Database Column missmatch.");
	        }

	    } catch (SQLException e) {
	        log.error("❌ Failed to fetch table metadata for '{}': {}", dbTable.getTableName(), e.getMessage(), e);
	    }
	}

	public void printColumns() { // this is giving correct output
		getAllColumnNamesEvenIfEmpty().forEach(System.out::println);
	}

	public boolean existsByDocId(String docId) {
		String sql = "SELECT COUNT(*) FROM " + dbTable.getTableName() + " WHERE doc_id = ?";
		try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, docId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) > 0;
			}
		} catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.toString()); 
		}
	}

	public Map<String, Integer> getColumnTypes(Connection conn) throws SQLException {
	    Map<String, Integer> columnTypes = new HashMap<>();
	    try (ResultSet rs = conn.getMetaData().getColumns(null, null, dbTable.getTableName().toUpperCase(), null)) {
	        while (rs.next()) {
	            columnTypes.put(rs.getString("COLUMN_NAME").toLowerCase(), rs.getInt("DATA_TYPE"));
	        }
	    }
	    return columnTypes;
	}
	public Map<String, String> extractDataMap(JsonNode jsonNode) {
	    Map<String, String> dataMap = new LinkedHashMap<>();
	    jsonNode.fieldNames().forEachRemaining(field -> {
	        JsonNode valueNode = jsonNode.get(field);
	        String value = (valueNode == null || valueNode.isNull()) ? null : valueNode.toString(); // ✅ Use toString()
	        dataMap.put(field.toLowerCase(), value);
	    });
	    return dataMap;
	}
	
}