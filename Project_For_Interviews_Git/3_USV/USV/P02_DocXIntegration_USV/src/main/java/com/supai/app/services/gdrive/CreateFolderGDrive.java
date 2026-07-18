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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supai.app.config.GdriveApiConfig;
import com.supai.app.config.GdriveCredentials;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CreateFolderGDrive {
	private final RestTemplate restTemplate;
	private final GdriveCredentials gdriveCredentials;
	private final GdriveApiConfig gdriveApiConfig;
	private final ObjectMapper objectMapper;
	private final CallGDriveApi callGDriveApi;
	private final JsonObj jsonObj;
	

	private ResponseEntity<String> callCreateFolderApi(String folderName, String rootFolderId) throws Exception {
		String url = gdriveApiConfig.getCreateFolderUri();// "https://www.googleapis.com/drive/v3/files";

		// Build JSON request body using ObjectNode
		ObjectNode requestBody = objectMapper.createObjectNode();
		requestBody.put("name", folderName);
		requestBody.put("mimeType", "application/vnd.google-apps.folder");
		requestBody.putArray("parents").add(rootFolderId);

		// Prepare headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(gdriveCredentials.getAccessToken()); // Bearer token for Authorization

		HttpEntity<JsonNode> requestEntity = new HttpEntity<>(requestBody, headers);
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

		return response;
	}
	public String createFolder(String folderToFind, String rootFolderId) {
		// Attempt to create a folder in Google Drive, retrying if unauthorized
		ResponseEntity<String> response = callGDriveApi.retryIfUnauthorizedGdrive(
				() -> this.callCreateFolderApi(folderToFind, rootFolderId),
				"Faild to Create Folder " + folderToFind, null);// "Successfully created Folder " + folderToFind);

		if (response != null) {
			// Parse the response to get the new folder ID
			JsonNode newFolderNode = jsonObj.getJson(response.getBody());
			String folderId = newFolderNode.get("id").asText();
			return folderId;
		} else {
			return null;
		}
	}
}
