package com.supai.app.services.gdrive;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.GdriveApiConfig;
import com.supai.app.config.GdriveCredentials;
import com.supai.app.dao.dto.DocumentRequest;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadFileContentGDrive {
	private final RestTemplate restTemplate;
	private final GdriveCredentials gdriveCredentials;
	private final GdriveApiConfig gdriveApiConfig;
	private final ObjectMapper objectMapper;
	private final ResumableUploadGDrive resumableUploadGDrive;

	private final CallGDriveApi callGDriveApi;
	private final JsonObj jsonObj;

	public ResponseEntity<JsonNode> uploadFileContent(String uploadUrl, ResponseEntity<byte[]> response)
			throws Exception {
		log.info("uploading doc to gdrive..");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setBearerAuth(gdriveCredentials.getAccessToken()); // Bearer token for Authorization

		// Prepare the file data from ResponseEntity<byte[]>
		byte[] fileContent = response.getBody();

		HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileContent, headers);

		ResponseEntity<JsonNode> uploadResponse = restTemplate.exchange(uploadUrl, HttpMethod.PUT, requestEntity,
				JsonNode.class);

		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(uploadResponse.getBody());
	}

	public ResponseEntity<JsonNode> uploadToGdrive(ResponseEntity<byte[]> otcsDocResponse, String spendCategory,
			String docName, String rootFolderIdInGdrive, String spendCategoryIdInGdrive, DocumentRequest request) {

		// Step 1: Start a resumable upload session in Google Drive
		ResponseEntity<JsonNode> uploadUrlResponse = callGDriveApi.retryIfUnauthorizedGdrive(
				() -> resumableUploadGDrive.initiateResumableUpload(docName, spendCategoryIdInGdrive),
				"Faild to get upload url", null);

		if (uploadUrlResponse != null) {
			// Step 2: Upload file content using the obtained upload URL
			String uploadUrl = uploadUrlResponse.getHeaders().getLocation().toString();
			ResponseEntity<JsonNode> uploadResponse = callGDriveApi.retryIfUnauthorizedGdrive(
					() -> this.uploadFileContent(uploadUrl, otcsDocResponse), "Faild to upload doc",
					"Successfully uploaded document " + request.getOtcsDocId() + " with name as '" + docName + "'");
			return uploadResponse;
		}
		return uploadUrlResponse;
	}


	public ResponseEntity<JsonNode> uploadToGdrive(ResponseEntity<byte[]> otcsDocResponse, String docName,
			String parentFolderId, DocumentRequest request) {

		// Step 1: Start a resumable upload session in Google Drive
		ResponseEntity<JsonNode> uploadUrlResponse = callGDriveApi.retryIfUnauthorizedGdrive(
				() -> resumableUploadGDrive.initiateResumableUpload(docName, parentFolderId), "Faild to get upload url",
				null);

		if (uploadUrlResponse != null) {
			// Step 2: Upload file content using the obtained upload URL
			String uploadUrl = uploadUrlResponse.getHeaders().getLocation().toString();
			ResponseEntity<JsonNode> uploadResponse = callGDriveApi.retryIfUnauthorizedGdrive(
					() -> this.uploadFileContent(uploadUrl, otcsDocResponse), "Faild to upload doc",
					"Successfully uploaded document " + request.getOtcsDocId() + " with name as '" + docName + "'");
			return uploadResponse;
		}
		return uploadUrlResponse;
	}
}
