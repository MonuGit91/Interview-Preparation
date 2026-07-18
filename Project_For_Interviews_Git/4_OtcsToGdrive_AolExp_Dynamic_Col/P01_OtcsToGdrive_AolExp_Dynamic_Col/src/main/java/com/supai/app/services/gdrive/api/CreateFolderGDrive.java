package com.supai.app.services.gdrive.api;

import java.util.List;

import org.springframework.stereotype.Component;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateFolderGDrive {
	private final Drive drive;

	private File driveCreateFolder(String folderName, String parentFolderId) throws Exception {
		File fileMetadata = new File();
		fileMetadata.setName(folderName);
		fileMetadata.setMimeType("application/vnd.google-apps.folder");
		fileMetadata.setParents(List.of(parentFolderId));

		return drive.files().create(fileMetadata).setFields("id, name").execute();
	}
	
	public File create(String folderName, String parentFolderId) {
		// Attempt to create a folder in Google Drive, retrying if unauthorized
		try {
			File folder = driveCreateFolder(folderName, parentFolderId);
	        //System.out.println("Folder ID: " + folder.getId());
			return folder;
		} catch (Exception e) {
			log.error("{}", e.toString());
		}
		
		return null;
	}
	public String createFolder(String folderName, String parentFolderId) {
		try {
			File folder = driveCreateFolder(folderName, parentFolderId);
			return folder.getId();
		} catch (Exception e) {
			log.error("{}", e.toString());
		}
		
		return null;
	}
}
