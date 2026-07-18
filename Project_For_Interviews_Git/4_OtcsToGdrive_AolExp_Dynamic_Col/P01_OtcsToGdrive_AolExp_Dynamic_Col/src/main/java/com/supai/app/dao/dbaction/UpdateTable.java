package com.supai.app.dao.dbaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.supai.app.config.DbTable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateTable {
	private final DbTable dbTable;
	private final DbConnection dbConnection;
	private final DbOperationCommon dbOperationCommon;

	private record UpdateQueryData(String setClause, List<String> columns, List<Object> values) {
//		You must be using Java 16 or later
//		Benefits of record:
//			Automatically provides:
//			Constructor
//			Getters (columnList(), columns(), etc.)
//			toString(), equals(), hashCode()
//			Ideal for data containers (DTOs, response objects, etc.)
	}

	public void updateNameByDocId(String newName, String docId) {
		String updateQuery = "UPDATE " + dbTable.getTableName() + " SET name = ? WHERE doc_id = ?";

		try (Connection conn = dbConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(updateQuery)) {
			ps.setString(1, newName);
			ps.setString(2, docId);

			int updatedRows = ps.executeUpdate();
			System.out.println("Updated rows: " + updatedRows);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int updateRow(Map<String, String> dataMap) { // it will update 1 row at a time
		if (!dbConnection.isTableExists())
			return 0;

		int updatedRowCount = 0;

		// set upload date
		String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		dataMap.put("upload_date", currentTimestamp);

		// Remove entries with null or blank values
		dataMap.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isBlank());

		Object docId = dataMap.get("doc_id");

		if (docId == null || docId.toString().isBlank()) {
			throw new IllegalArgumentException("doc_id is required for update");
		}

		try (Connection conn = dbConnection.getConnection()) {
			Map<String, Integer> columnTypes = dbOperationCommon.getColumnTypes(conn);
			UpdateQueryData updateData = buildUpdateData(dataMap, columnTypes);
			updatedRowCount += executeUpdate(conn, updateData, docId);
		} catch (Exception e) {
			log.error("Failed to update record with doc_id=" + docId + ": " + e.getMessage());
		}

		return updatedRowCount;
	}

	private UpdateQueryData buildUpdateData(Map<String, String> dataMap, Map<String, Integer> columnTypes) {
		List<String> columns = new ArrayList<>();
		List<Object> values = new ArrayList<>();

		for (Map.Entry<String, String> entry : dataMap.entrySet()) {
			String col = entry.getKey();
			if (!"doc_id".equalsIgnoreCase(col)) {
				columns.add(col);
				values.add(entry.getValue());
			}
		}

		String setClause = columns.stream().map(col -> col + " = ?").collect(Collectors.joining(", "));
		return new UpdateQueryData(setClause, columns, values);
	}

	private int executeUpdate(Connection conn, UpdateQueryData updateData, Object docId) throws SQLException {
		String sql = "UPDATE " + dbTable.getTableName() + " SET " + updateData.setClause + " WHERE doc_id = ?";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			Map<String, Integer> columnTypes = dbOperationCommon.getColumnTypes(conn);

			for (int i = 0; i < updateData.values.size(); i++) {
				String colName = updateData.columns.get(i);
				Object value = updateData.values.get(i);
				int sqlType = columnTypes.getOrDefault(colName, Types.VARCHAR);

				if (value == null) {
					ps.setNull(i + 1, sqlType);
				} else {
					switch (sqlType) {
					case Types.VARCHAR, Types.CHAR -> ps.setString(i + 1, value.toString());
					case Types.INTEGER -> ps.setInt(i + 1, Integer.parseInt(value.toString()));
					case Types.BIGINT -> ps.setLong(i + 1, Long.parseLong(value.toString()));
					case Types.BOOLEAN, Types.BIT -> ps.setBoolean(i + 1, Boolean.parseBoolean(value.toString()));
					case Types.DATE -> ps.setDate(i + 1, java.sql.Date.valueOf(value.toString()));
					case Types.TIMESTAMP -> ps.setTimestamp(i + 1, java.sql.Timestamp.valueOf(value.toString()));
					default -> ps.setObject(i + 1, value);
					}
				}
			}

			ps.setObject(updateData.values.size() + 1, docId); // WHERE doc_id = ?
			int rowsUpdated = ps.executeUpdate();

			return rowsUpdated;
		}
	}

	public void updateRecord(Connection conn, Map<String, Object> dataMap, String docId) throws SQLException {
		List<String> columns = new ArrayList<>();
		List<Object> values = new ArrayList<>();

		for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
			if (!"doc_id".equalsIgnoreCase(entry.getKey())) {
				columns.add(entry.getKey());
				values.add(entry.getValue());
			}
		}

		String setClause = columns.stream().map(col -> col + " = ?").collect(Collectors.joining(", "));
		String sql = "UPDATE " + dbTable.getTableName() + " SET " + setClause + " WHERE doc_id = ?";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			for (int i = 0; i < values.size(); i++) {
				ps.setObject(i + 1, values.get(i));
			}
			ps.setString(values.size() + 1, docId); // where clause
			ps.executeUpdate();
		}
	}

}