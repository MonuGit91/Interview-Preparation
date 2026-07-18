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
public class InsertInTable {
	private final DbTable dbTable;
	private final DbConnection dbConnection;
	private final DbOperationCommon dbOperationCommon;

	public record InsertQueryData(String columnList, String placeholderList, List<String> columns,
			List<Object> values) {
	}

	public int insertRows(Map<String, String> dataMap) {
		if (!dbConnection.isTableExists())
			return 0;

		int insertedRowCount = 0;

		// Set current timestamp for upload_date
		String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		dataMap.put("upload_date", currentTimestamp);

		// Remove entries with null or blank values
		dataMap.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isBlank());

		try (Connection conn = dbConnection.getConnection()) {
			Map<String, Integer> columnTypes = dbOperationCommon.getColumnTypes(conn);
			InsertQueryData insertData = buildInsertData(dataMap, columnTypes);
			insertedRowCount += executeInsert(conn, insertData);
		} catch (Exception e) {
			log.error("Failed to insert record: " + e.getMessage());
		}
		
		return insertedRowCount;
	}

	private InsertQueryData buildInsertData(Map<String, String> dataMap, Map<String, Integer> columnTypes) {
		List<String> columns = new ArrayList<>();
		List<Object> values = new ArrayList<>();

		for (Map.Entry<String, String> entry : dataMap.entrySet()) {
			columns.add(entry.getKey());
			values.add(entry.getValue());
		}

		String columnList = String.join(", ", columns);
		String placeholderList = columns.stream().map(col -> "?").collect(Collectors.joining(", "));

		return new InsertQueryData(columnList, placeholderList, columns, values);
	}

	private int executeInsert(Connection conn, InsertQueryData insertData) throws SQLException {
		String sql = "INSERT INTO " + dbTable.getTableName() + " (" + insertData.columnList + ") VALUES ("
				+ insertData.placeholderList + ")";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			Map<String, Integer> columnTypes = dbOperationCommon.getColumnTypes(conn);

			for (int i = 0; i < insertData.values.size(); i++) {
				String colName = insertData.columns.get(i);
				Object value = insertData.values.get(i);
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

			return ps.executeUpdate();
		}
	}
}