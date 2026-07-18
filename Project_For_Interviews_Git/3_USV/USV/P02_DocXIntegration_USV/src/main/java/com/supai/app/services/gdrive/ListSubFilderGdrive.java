package com.supai.app.services.gdrive;

import java.net.URI;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.GdriveApiConfig;
import com.supai.app.config.GdriveCredentials;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ListSubFilderGdrive {
	private final RestTemplate restTemplate;
	private final GdriveCredentials gdriveCredentials;
	private final GdriveApiConfig gdriveApiConfig;
	private final CallGDriveApi callGDriveApi;
	private final JsonObj jsonObj;
	
	private ResponseEntity<String> listSubFolders(String rootFolderId) throws Exception {
		// Build the query parameters safely using UriComponentsBuilder
		String url = gdriveApiConfig.getListSubfoldersUri();
		String baseQueryParam = "mimeType='application/vnd.google-apps.folder' and 'folderId' in parents and trashed=false";

		URI finalUrl = UriComponentsBuilder.fromHttpUrl(url)
				.queryParam("q", baseQueryParam.replace("folderId", rootFolderId))
				.queryParam("fields", "files(id,name)").build().toUri();

		// Execute the GET request
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(gdriveCredentials.getAccessToken());

		// Create an entity with the headers
		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange(finalUrl, HttpMethod.GET, entity, String.class);

		return response;
	}
	
	public JsonNode fetchSubFolders(String rootFolderId) {
		// Send a request to list subfolders of the specified root folder in Google
		// Drive
		ResponseEntity<String> response =  callGDriveApi.retryIfUnauthorizedGdrive(() -> this.listSubFolders(rootFolderId), null, null);

		if (response != null) {
			return jsonObj.getJson(response.getBody().toString());
		} else {
			return null;
		}
	}
}
