package com.supai.app.services.gdrive;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.supai.app.config.GdriveCredentials;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionApiGdrive {
	private final CallingGDriveApi callingGDriveApi;
	private final GdriveCredentials gdriveCredentials;
	private final RestTemplate restTemplate; // Injected from Spring config
	private final JsonObj jsonObj; // Injected from Spring config

	private static String API_URL = "https://www.googleapis.com/drive/v3/files/{fileId}/permissions";

	// Replace with a valid token (you may inject via config/service)

	private ResponseEntity<String> shareFileWithUser(String fileId, String email, boolean writeAccess) throws Exception{
		// Request body for permissions
		Map<String, Object> requestBody = new HashMap<>();
		if(writeAccess) {
			requestBody.put("role", "writer"); 
		} else {
			requestBody.put("role", "reader");
		}
		requestBody.put("type", "user");
		requestBody.put("emailAddress", email);

		// Headers
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(gdriveCredentials.getAccessToken());
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

	    ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, request, String.class,
				fileId);
	    
	    if(response.getStatusCode().is2xxSuccessful()) {
	    	return response;
	    } else {
	    	throw new RuntimeException(response.getBody());
	    }
	}
	
	public ResponseEntity<String> grantPermission(String fileId, String email, boolean writeAccess) throws Exception {
		String errorMsg = "not able to share file with "+email;
		String successMsg = "permission guarended.";
		try {
			return callingGDriveApi.retryIfUnauthorizedGdrive(() -> this.shareFileWithUser(fileId, email, writeAccess), errorMsg, successMsg);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage()); 
		}
		
	}
}
