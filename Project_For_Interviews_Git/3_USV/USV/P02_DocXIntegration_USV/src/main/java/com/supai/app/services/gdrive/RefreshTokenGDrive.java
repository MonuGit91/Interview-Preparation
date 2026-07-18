package com.supai.app.services.gdrive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.supai.app.config.GdriveApiConfig;
import com.supai.app.config.GdriveCredentials;
import com.supai.app.exception.IllegalClientSecretException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenGDrive {
	private final Environment environment;
	private final GdriveCredentials gdriveCredentials;
	private final String TOKENS_DIRECTORY_PATH = "tokens";
	private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private final GdriveApiConfig gdriveApiConfig;
//    private final List<String> SCOPES = List.of("https://www.googleapis.com/auth/drive.appdata", "https://www.googleapis.com/auth/drive.file", "https://www.googleapis.com/auth/drive");

	private Credential callRefreshTokenApi() throws IOException, GeneralSecurityException {
		InputStream clientSecretRaw = new FileInputStream(gdriveCredentials.getCredentialsFilePath());
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
				new InputStreamReader(clientSecretRaw));
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets,
				gdriveCredentials.getScopes())
				.setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH))).setAccessType("offline")
				.setApprovalPrompt("force") // 🔁 Force re-consent to ensure refresh token
				.build();
		int gdriveAppPort = Integer.parseInt(environment.getProperty("gdrive.app-port")); // or hard doce any port like
		// 8888
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(gdriveAppPort).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}
	

	public ResponseEntity<JsonNode> setRefreshToken() throws Exception {
		try {
			Credential credential = callRefreshTokenApi();
			gdriveCredentials.setRefreshToken(credential.getRefreshToken());
			
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode jsonBody = mapper.createObjectNode();
			jsonBody.put("refresh_token", gdriveCredentials.getRefreshToken());

			HttpHeaders headers = new HttpHeaders();
			ResponseEntity<JsonNode> response = new ResponseEntity<>(jsonBody, headers, HttpStatus.OK);

			return response;
		} catch (Exception e) {
			String msg = e.getMessage();
			if (msg == null && e.getClass().descriptorString().contains("IllegalArgumentException")) {
				e = new IllegalClientSecretException();
			}
			throw e;
		}
	}
}
