package com.supai.app.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class DocMetadataService {
	private final DataSource dataSource;
	
	public void insertJsonNodeIntoDB(JsonNode jsonMetadata, String tableName) {
        try (Connection conn = dataSource.getConnection()) {

            List<String> columns = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            Iterator<String> fieldNames = jsonMetadata.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                JsonNode valueNode = jsonMetadata.get(field);

                columns.add(field);
                values.add(valueNode.isNull() ? null : valueNode.asText());
            }

            String columnPart = String.join(", ", columns);
            String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
            String sql = "INSERT INTO " + tableName + " (" + columnPart + ") VALUES (" + placeholders + ")";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < values.size(); i++) {
                    ps.setObject(i + 1, values.get(i));
                }
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database insert failed", e);
        }
    }
}
