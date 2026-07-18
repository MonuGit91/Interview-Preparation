package com.supai.app.services.gdrive;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;

import com.supai.app.config.GdriveCredentials;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DeleteDocGDrive {
	private final GdriveCredentials gdriveCredentials;
	private final CallingGDriveApi callingGDriveApi;

	private ResponseEntity<Void> deleteFile(String fileId) throws Exception{
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + gdriveCredentials.getAccessToken());

		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<Void> response = restTemplate.exchange("https://www.googleapis.com/drive/v3/files/{fileId}",
				HttpMethod.DELETE, entity, Void.class, fileId);

		if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
			return ResponseEntity.ok(response.getBody());
		}

		throw new RuntimeException("Failed to delete file. Status: " + response.getStatusCode());
	}

	public ResponseEntity<Void> removeDriveDoc(String fileId) throws Exception{
		try {
			return callingGDriveApi.retryIfUnauthorizedGdrive(() -> this.deleteFile(fileId),
					"Failed to delete doc having id: " + fileId, "Successfully deleted doc having id: " + fileId);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
		
	}
//	public ResponseEntity<Void> removeDriveDoc_(String fileId) {
//		return callGDriveApi.retryIfUnauthorizedGdrive(() -> this.deleteFile(fileId),
//				"Failed to delete doc having id: " + fileId, "Successfully deleted doc having id: " + fileId);
//	}
}
