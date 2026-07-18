package com.supai.app.services.otcs;

import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.supai.app.config.OtcsApi;
import com.supai.app.config.OtcsCredentials;
import com.supai.app.dao.dto.VersionRequest;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtcsUploader {

	private final OtcsCredentials otcsCredentials;
	private final CallingOtcsApi callingOtcsApi;
	private final OtcsApi otcsApi;
	private final JsonObj jsonObj;

	private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build();

	public ResponseEntity<String> addVersion(String baseUrl, VersionRequest parameter, ResponseEntity<byte[]> fileBytes)
			throws Exception {

		// Build multipart body
		RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("file", parameter.getName(),
						RequestBody.create(fileBytes.getBody(), MediaType.parse("application/octet-stream")))
				.build();

		String url = baseUrl + otcsApi.getAddVersion().replace("{id}", parameter.getOtcsDocId());
		// Build request
		Request request = new Request.Builder().url(url).post(body).addHeader("otcsticket", parameter.getOtcsTicket())
				.build();

		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) {
			throw new RuntimeException(response.message());
		}

		String responseBody = response.body() != null ? response.body().string() : null;
		return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.body(responseBody);
	}

	public ResponseEntity<String> addVersionToDoc(VersionRequest request, ResponseEntity<byte[]> fileBytes) throws Exception{
		log.info("Adding version to doc...");
		try {
			ResponseEntity<String> response = addVersion(request.getBaseUrl(), request, fileBytes);
			log.info("Add version response: " + response.getBody());
			return response;
		} catch (Exception e) {
			String error = jsonObj.getJson("error", "Faild adding version to document.", e.getMessage()).toString();
			log.error(error);
			throw new RuntimeException(error);
		}
		
	}
	public ResponseEntity<String> addVersionToDoc_(VersionRequest request, ResponseEntity<byte[]> fileBytes) {
		log.info("Adding version to doc...");
		ResponseEntity<String> response = callingOtcsApi.callWithRetry(() -> addVersion(request.getBaseUrl(), request, fileBytes), String.class);
		log.info("Update version response: " + response.getBody());
		if (callingOtcsApi.isUnExpectedResponse(response)) {
			log.error("Faild to add version to document - {}", response.getBody());
			return null;
		}
		return response;
	}
}
