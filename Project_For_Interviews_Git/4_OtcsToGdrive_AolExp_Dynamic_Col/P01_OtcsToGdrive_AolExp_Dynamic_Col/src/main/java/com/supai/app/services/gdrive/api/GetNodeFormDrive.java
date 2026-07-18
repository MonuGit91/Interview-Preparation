package com.supai.app.services.gdrive.api;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetNodeFormDrive {
	private final Drive drive;
	
	private File callGetInfo(String fileId) throws IOException {
		return drive.files().get(fileId).setFields("id, name, mimeType, parents, owners, createdTime")
.execute();
	}
	public File getNodeInfo(String fileId) {
		try {
			return callGetInfo(fileId);
		} catch (Exception e) {
			log.error("{}", e.toString());
			return null;
		}
	}
}
