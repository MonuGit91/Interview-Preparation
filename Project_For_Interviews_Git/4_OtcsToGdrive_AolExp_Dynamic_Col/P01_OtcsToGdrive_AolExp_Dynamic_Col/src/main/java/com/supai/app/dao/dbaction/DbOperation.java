package com.supai.app.dao.dbaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbOperation {
	private final UpdateTable updateTable;
	private final InsertInTable insertInTable;
	private final DbOperationCommon dbOperationCommon;
	
	public void addDataToTable(List<JsonNode> jsonMetadata) {
		List<Map<String, String>> metadataMap = new ArrayList<>();
		jsonMetadata.forEach(jsonMetaData -> {
			metadataMap.add(dbOperationCommon.extractDataMap(jsonMetaData));
		});
			
		
		
		int updatedRowConut[] = {1};
		metadataMap.forEach(metaData -> {
			if(updatedRowConut[0] == 1 ) {
				dbOperationCommon.isColumnCorrect(new ArrayList(metaData.keySet()));
			}
			
			boolean isDocExist = dbOperationCommon.existsByDocId(metaData.get("doc_id"));
			
			if(isDocExist) {
				updatedRowConut[0] += updateTable.updateRow(metaData);
			}
			else {
				updatedRowConut[0] += insertInTable.insertRows(metaData);
			}
		});
		
		log.info("====> Total {} Row updated in Table", updatedRowConut[0]);
	}
}
