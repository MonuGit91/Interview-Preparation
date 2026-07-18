package com.supai.app.services.gdrive;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;

import com.supai.app.config.GdriveApiConfig;
import com.supai.app.config.GdriveCredentials;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GDriveDownloader {
	private final GdriveApiConfig gdriveApiConfig;
	private final GdriveCredentials gdriveCredentials;
	private final CallingGDriveApi callingGDriveApi;
	private final JsonObj jsonObj;

	private ResponseEntity<byte[]> downloadFile(String fileId) {
		RestTemplate restTemplate = new RestTemplate();

		// Set Authorization header
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + gdriveCredentials.getAccessToken());

		HttpEntity<String> entity = new HttpEntity<>(headers);

		// Call Google Drive API
		ResponseEntity<byte[]> response = restTemplate.exchange(gdriveApiConfig.getDownloadDriveDocUri(),
				HttpMethod.GET, entity, byte[].class, fileId);

		if (response.getStatusCode() == HttpStatus.OK) {
			byte[] fileBytes = response.getBody();
			if (fileBytes != null) {
				return response;
			}
		}

		throw new RuntimeException("Failed to download file. Status: " + response.getStatusCode());
	}
	
	public ResponseEntity<byte[]> getDriveDoc(String fileId) throws Exception{
		 try {
			 ResponseEntity<byte[]> downloadResponse = callingGDriveApi.retryIfUnauthorizedGdrive(() -> this.downloadFile(fileId),
						"Faild to download doc having id: " + fileId,
						"Successfully downloaded doc having id: " + fileId);
			 log.info("Successfully downloaded doc having id: {}", fileId);
			 return downloadResponse;
		 } catch (Exception e) {
			 // e.getMessage() will be in json format because that is handled
			 throw new IllegalAccessException(e.getMessage());
		 }
		 
	 }
	
//	 public ResponseEntity<byte[]> getDriveDoc_(String fileId) {
//		 ResponseEntity<byte[]> downloadResponse = callGDriveApi.retryIfUnauthorizedGdrive(() -> this.downloadFile(fileId),
//					"Faild to download doc having id: " + fileId,
//					"Successfully downloaded doc having id: " + fileId);
//		 
//		 return downloadResponse;
//		 
//	 }
}
