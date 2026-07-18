package com.supai.app.services.gdrive.api;

import java.io.IOException;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteNodeFormDrive {
	private final Drive drive;

	public void deleteFile(String fileId) throws IOException {
		drive.files().delete(fileId).execute();
	}
}
