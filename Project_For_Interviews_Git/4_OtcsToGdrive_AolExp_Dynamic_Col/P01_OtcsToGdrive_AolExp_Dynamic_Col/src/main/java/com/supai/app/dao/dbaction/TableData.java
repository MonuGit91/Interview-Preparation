package com.supai.app.dao.dbaction;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.stereotype.Component;

import com.supai.app.config.DbTable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TableData {
	private final DbTable dbTable;
	private final DbConnection dbConnection;

	public void printAllDataFromTable() {
		String query = "SELECT * FROM " + dbTable.getTableName();
		System.out.println(query);
		try (Connection conn = dbConnection.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(query)) {
			int columnCount = rs.getMetaData().getColumnCount();

			while (rs.next()) {
				for (int i = 1; i <= columnCount; i++) {
					String colName = rs.getMetaData().getColumnName(i).toLowerCase();
					String value = rs.getString(i);
					System.out.print(colName + ": " + value + " | ");
					System.out.print(colName + " | ");
				}
				System.out.println(); // new line for each row
				break;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}