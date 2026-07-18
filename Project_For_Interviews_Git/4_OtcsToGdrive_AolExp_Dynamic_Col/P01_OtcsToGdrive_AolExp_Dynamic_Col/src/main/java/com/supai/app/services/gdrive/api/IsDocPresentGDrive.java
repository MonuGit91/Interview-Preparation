package com.supai.app.services.gdrive.api;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IsDocPresentGDrive {
	private final Drive drive;

	private File getFileByNameInFolder(String fileName, String parentFolderId) throws IOException {
		String query = String.format("name = '%s' and '%s' in parents and trashed = false",
				fileName.replace("'", "\\'"), // Escape single quotes
				parentFolderId);

		FileList result = drive.files().list().setQ(query).setSpaces("drive").setFields("files(id, name)").execute();

		List<File> files = result.getFiles();
		return (files != null && !files.isEmpty()) ? files.get(0) : null;
	}

	public File isDocPresentRwa(String docName, String folderId) {
		try {
			return getFileByNameInFolder(docName, folderId);
		} catch (Exception e) {
			return null;
		}
	}

	public ResponseEntity<String> isDocPresentOnGdrive(String docName, String folderId) {
		try {
			File file = getFileByNameInFolder(docName, folderId);

			ObjectMapper mapper = new ObjectMapper();
			ObjectNode root = mapper.createObjectNode();
			ArrayNode filesArray = mapper.createArrayNode();

			if (file != null) {
				ObjectNode fileNode = mapper.createObjectNode();
				fileNode.put("id", file.getId());
				fileNode.put("name", file.getName());
				filesArray.add(fileNode);
			}

			root.set("files", filesArray);

			return ResponseEntity.ok().header("Content-Type", "application/json").body(mapper.writeValueAsString(root));

		} catch (Exception e) {
			return null;
		}
	}

}
