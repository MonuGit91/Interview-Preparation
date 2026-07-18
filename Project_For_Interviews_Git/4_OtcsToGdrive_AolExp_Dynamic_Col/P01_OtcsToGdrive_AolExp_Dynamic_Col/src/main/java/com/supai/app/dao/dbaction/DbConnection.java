package com.supai.app.dao.dbaction;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.supai.app.config.DbCredentials;
import com.supai.app.config.DbTable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbConnection {
	private final DbCredentials dbCredentials;
	private final DataSource dataSource;
	private final DbTable dbTable;
	
	public Connection getConnection() {
		try {
			return DriverManager.getConnection(dbCredentials.getUrl(), dbCredentials.getUsername(),
					dbCredentials.getPassword());
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean isTableExists() {
		try (Connection conn = dataSource.getConnection()) {
			DatabaseMetaData metaData = conn.getMetaData();
			try (ResultSet rs = metaData.getTables(null, null, dbTable.getTableName().toUpperCase(), new String[] { "TABLE" })) {
				return rs.next();
			}
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
		}
	}
}
