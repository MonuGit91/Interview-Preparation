package com.supai.app.services.gdrive;

import java.net.URI;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supai.app.config.GdriveApiConfig;
import com.supai.app.config.GdriveCredentials;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResumableUploadGDrive {
	private final RestTemplate restTemplate;
	private final GdriveCredentials gdriveCredentials;
	private final GdriveApiConfig gdriveApiConfig;
	private final ObjectMapper objectMapper;
	private final CallGDriveApi callGDriveApi;
	private final JsonObj jsonObj;
	
	public ResponseEntity<JsonNode> initiateResumableUpload(String fileName, String parentId) throws Exception {
		String url = gdriveApiConfig.getResumableUploadUri();

		// Prepare metadata for the file to be uploaded
		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("name", fileName);
		metadata.put("mimeType", "application/octet-stream"); // Update based on your file type
		metadata.putArray("parents").add(parentId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(gdriveCredentials.getAccessToken()); // Bearer token for Authorization
		headers.set("X-Upload-Content-Type", "application/octet-stream");

		HttpEntity<JsonNode> requestEntity = new HttpEntity<>(metadata, headers);

		ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, requestEntity, JsonNode.class);

		return response;
	}
}
