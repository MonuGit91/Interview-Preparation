package com.supai.app.services.gdrive.api;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListSubFilesGDrive {
	private final Drive drive;

	private List<File> callListFilesInFolderApi(String folderId) throws IOException {
		String query = String.format("'%s' in parents and trashed = false", folderId);

		FileList result = drive.files().list().setQ(query).setFields("files(id, name, mimeType, owners)").execute();

		return result.getFiles(); // List of File objects
	}
	public List<File> listFiles(String folderId){
		try {
			return callListFilesInFolderApi(folderId);
		} catch(Exception e) {
			log.error("{}", e.toString());
		}
		return null;
	}
}
