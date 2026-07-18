package com.supai.app.services.gdrive;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.GdriveApiConfig;
import com.supai.app.config.GdriveCredentials;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccessTokenGDrive {
	private final ObjectMapper objectMapper;
	private final GdriveCredentials gdriveCredentials;
	private final RestTemplate restTemplate;
	private final RefreshTokenGDrive refreshTokenGDrive;
	private final GdriveApiConfig gdriveApiConfig;

	public ResponseEntity<String> updateAccessToken() throws Exception {
	    // Prepare headers
	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

	    // Prepare body parameters
	    MultiValueMap<String, String> bodyParams = new LinkedMultiValueMap<>();
	    bodyParams.add("client_id", gdriveCredentials.getClientId());
	    bodyParams.add("client_secret", gdriveCredentials.getClientSecret());
	    bodyParams.add("refresh_token", gdriveCredentials.getRefreshToken());
	    bodyParams.add("grant_type", gdriveCredentials.getGrantType());

	    HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(bodyParams, headers);

	    // Send request
	    ResponseEntity<String> response = restTemplate.exchange(gdriveApiConfig.getAccessTokenUri(), HttpMethod.POST, entity, String.class);

	    // Parse response and update token
	    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
	    String newAccessToken = jsonResponse.path("access_token").asText();

	    // Update in your credentials bean if needed
	    gdriveCredentials.setAccessToken(newAccessToken);

	    return response;
	}


}
