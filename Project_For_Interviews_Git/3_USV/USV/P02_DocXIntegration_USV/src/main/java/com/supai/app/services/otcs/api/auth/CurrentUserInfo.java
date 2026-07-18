package com.supai.app.services.otcs.api.auth;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.config.OtcsApi;
import com.supai.app.config.OtcsCredentials;
import com.supai.app.dao.dto.DocMetadata;
import com.supai.app.services.common.JsonObj;
import com.supai.app.services.otcs.CallingOtcsApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentUserInfo {
	private final OtcsCredentials otcsCredentials;
	private final CallingOtcsApi callingOtcsApi;
	private final OtcsApi otcsApi;
	private final JsonObj jsonObj;

	private final OkHttpClient client;

	private ResponseEntity<JsonNode> currentUserInfo(DocMetadata docMetadata) throws Exception {
		Request request = new Request.Builder().url(docMetadata.getBaseUrl() + otcsApi.getAuth().getAuthApi())
				.method("GET", null).addHeader("otcsticket", docMetadata.getOtcsTicket()).build();

		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) {
			throw new RuntimeException(response.message());
		}

		String responseBody = response.body() != null ? response.body().string() : null;
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonObj.getJson(responseBody));
	}

	public ResponseEntity<JsonNode> getCurrentUserInfo_(DocMetadata docMetadata) {
		log.info("Adding version to doc...");
		ResponseEntity<JsonNode> response = callingOtcsApi.callWithRetry(() -> currentUserInfo(docMetadata),
				JsonNode.class);
		return response;
	}

	public ResponseEntity<JsonNode> getCurrentUserInfo(DocMetadata docMetadata) throws Exception {
		log.info("Adding version to doc...");
		try {
			ResponseEntity<JsonNode> response = callingOtcsApi.callWithRetry(() -> currentUserInfo(docMetadata),
					JsonNode.class);
			if(!response.getStatusCode().is2xxSuccessful()) {
				String errorJson = jsonObj.getJson("error", "error while fetching user info.", response.getBody().toString()).toString();
				log.error(errorJson);
				throw new RuntimeException(errorJson);
			}
			return response;
		} catch (Exception e) {
			String errorJson = jsonObj.getJson("error", "error while fetching user info.", e.getMessage()).toString();
			log.error(errorJson);
			throw new RuntimeException(errorJson);
		}
	}
}