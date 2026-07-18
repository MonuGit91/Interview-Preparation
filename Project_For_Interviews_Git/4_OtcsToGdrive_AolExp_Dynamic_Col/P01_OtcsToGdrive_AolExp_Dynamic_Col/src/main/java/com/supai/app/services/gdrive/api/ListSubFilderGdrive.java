package com.supai.app.services.gdrive.api;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListSubFilderGdrive {

	private final Drive drive;

	/**
	 * Fetches the subfolders (only folders) inside a given parent folder using
	 * Drive API.
	 */
	private FileList listSubFolders(String parntFolder) throws Exception {
		// Build query: only folders, not trashed, and inside given parent folder
		String query = String.format(
				"mimeType = 'application/vnd.google-apps.folder' and '%s' in parents and trashed = false",
				parntFolder);

		// Execute the query
		FileList result = drive.files().list().setQ(query).setFields("files(id, name)").execute();

		return result;
	}
	public FileList getSubFolders(String parntFolder) {
		try {
			FileList result = listSubFolders(parntFolder);
			return result;
		} catch (Exception e) {
			log.error("{}", e.toString());
		}
		return null;
	}
	
	public JsonNode fetchSubFolders(String rootFolderId) {
		try {
			FileList result = listSubFolders(rootFolderId);
			return convertToJson(result);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private JsonNode convertToJson(FileList result) {
		// Convert result to JsonNode
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode filesArray = mapper.createArrayNode();

		for (File file : result.getFiles()) {
			ObjectNode fileNode = mapper.createObjectNode();
			fileNode.put("id", file.getId());
			fileNode.put("name", file.getName());
			filesArray.add(fileNode);
		}
		ObjectNode root = mapper.createObjectNode();
		root.set("files", filesArray);

		return root;

	}
}
