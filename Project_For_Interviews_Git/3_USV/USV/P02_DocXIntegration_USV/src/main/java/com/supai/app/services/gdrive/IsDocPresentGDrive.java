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
public class IsDocPresentGDrive {
	private final RestTemplate restTemplate;
	private final GdriveCredentials gdriveCredentials;
	private final GdriveApiConfig gdriveApiConfig;
	private final CallGDriveApi callGDriveApi;
	
	private ResponseEntity<String> callIsDocPresentApi(String docName, String folderId) {
		// TODO Auto-generated method stub
		// Retrieve base URL from config
		String url = gdriveApiConfig.getListSubfoldersUri(); // "https://www.googleapis.com/drive/v3/files"

		// Create the query to search for the document by name in the specified folder
		String queryParam = "name='" + docName + "' and '" + folderId + "' in parents and trashed=false";

		// Build the final URL using UriComponentsBuilder
		URI finalUrl = UriComponentsBuilder.fromHttpUrl(url).queryParam("q", queryParam)
				.queryParam("fields", "files(id,name)") // Requesting specific fields: file ID and name
				.build().toUri();

		// Prepare headers (add Authorization Bearer token)
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(gdriveCredentials.getAccessToken()); // Replace with actual access token

		// Create HttpEntity containing headers
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		// Call the API
		ResponseEntity<String> response = restTemplate.exchange(finalUrl, HttpMethod.GET, requestEntity, String.class);

		return response;
	}
	
	public ResponseEntity<String> isDocPresentOnGdrive(String docName, String folderId) {
		ResponseEntity<String> responseIsDocExist =  callGDriveApi.retryIfUnauthorizedGdrive(
				() -> this.callIsDocPresentApi(docName, folderId), null, null);
		return responseIsDocExist;
	}
}
