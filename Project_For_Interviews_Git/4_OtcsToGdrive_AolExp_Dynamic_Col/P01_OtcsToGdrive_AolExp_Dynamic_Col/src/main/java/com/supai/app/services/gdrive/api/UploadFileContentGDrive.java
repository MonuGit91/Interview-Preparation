package com.supai.app.services.gdrive.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.supai.app.services.common.LogBuffer;
import com.supai.app.services.common.LogUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadFileContentGDrive {	
	private final Drive drive;

	public File uploadFile(java.io.File file, String parentFolderId) throws IOException {
		File fileMetadata = new File();
		fileMetadata.setName(file.getName());
		fileMetadata.setParents(List.of(parentFolderId));

		FileContent mediaContent = new FileContent("application/octet-stream", file);


	    // Prepare Drive upload request
	    Drive.Files.Create insert = drive.files().create(fileMetadata, mediaContent);
	    insert.setFields("id, name");

	    // Enable resumable upload
	    var uploader = insert.getMediaHttpUploader();
	    uploader.setDirectUploadEnabled(false); // Enables resumable upload
	    uploader.setChunkSize(10 * 1024 * 1024); // 10 MB chunks

//	    // Optional: progress listener with modern switch expression
//	    uploader.setProgressListener(u -> {
//	        switch (u.getUploadState()) {
//	            case INITIATION_STARTED -> System.out.println("Upload Initiation Started");
//	            case INITIATION_COMPLETE -> System.out.println("Upload Initiation Completed");
//	            case MEDIA_IN_PROGRESS -> System.out.printf("Upload in progress: %.2f%%%n", u.getProgress() * 100);
//	            case MEDIA_COMPLETE -> System.out.println("Upload Completed");
//	            case NOT_STARTED -> System.out.println("Upload Not Started");
//	        }
//	    });

	    // Execute the upload
	    return insert.execute();

	}
	
	public File uploadFile(ResponseEntity<byte[]> response, String fileName, String folderId) throws IOException {
	    // Create metadata
	    File fileMetadata = new File();
	    fileMetadata.setName(fileName);
	    fileMetadata.setParents(List.of(folderId));

	    // Convert byte[] to InputStream
	    byte[] fileBytes = response.getBody();
	    if (fileBytes == null) {
	        throw new IOException("Empty response body; no data to upload.");
	    }
	    ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);

	    // Use InputStreamContent with resumable upload
	    InputStreamContent mediaContent = new InputStreamContent("application/octet-stream", inputStream);
	    mediaContent.setLength(fileBytes.length); // Required for resumable uploads

	    // Prepare Drive upload request
	    Drive.Files.Create insert = drive.files().create(fileMetadata, mediaContent);
	    insert.setFields("id, name");

	    // Enable resumable upload
	    var uploader = insert.getMediaHttpUploader();
	    uploader.setDirectUploadEnabled(false); // Enables resumable upload
	    uploader.setChunkSize(10 * 1024 * 1024); // 10 MB chunks

//	    // Optional: progress listener with modern switch expression
//	    uploader.setProgressListener(u -> {
//	        switch (u.getUploadState()) {
//	            case INITIATION_STARTED -> System.out.println("Upload Initiation Started");
//	            case INITIATION_COMPLETE -> System.out.println("Upload Initiation Completed");
//	            case MEDIA_IN_PROGRESS -> System.out.printf("Upload in progress: %.2f%%%n", u.getProgress() * 100);
//	            case MEDIA_COMPLETE -> System.out.println("Upload Completed");
//	            case NOT_STARTED -> System.out.println("Upload Not Started");
//	        }
//	    });

	    // Execute the upload
	    return insert.execute();
	}
	
	public ResponseEntity<JsonNode> uploadToGdrive(ResponseEntity<byte[]> otcsDocResponse, String spendCategory, String docName,
			String docId, String rootFolderIdInGdrive, String spendCategoryIdInGdrive) {

		File uploadedFile = null;
		try {
			uploadedFile = uploadFile(otcsDocResponse, docName, spendCategoryIdInGdrive);
			ObjectNode jsonBody = new ObjectMapper().createObjectNode();
			jsonBody.put("id", uploadedFile.getId());
			LogBuffer.append(LogUtils.info("Uploading success with id : {}", uploadedFile.getId()));
			return new ResponseEntity<>(jsonBody, new HttpHeaders(), HttpStatus.OK);
		} catch (Exception e) {
			//log.error("{}", e.getMessage());
			LogBuffer.append(LogUtils.error("{}", e.toString()));
			return null;
		}
	}
}
