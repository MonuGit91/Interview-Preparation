package com.supai.app.services.otcs;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supai.app.config.OtcsApi;
import com.supai.app.dao.dto.DocMetadata;
import com.supai.app.dao.dto.DocumentRequest;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveToggle {
	private final OkHttpClient client;
	private final CallingOtcsApi callingOtcsApi;
	private final OtcsApi otcsApi;
	private final JsonObj jsonObj;

	private ResponseEntity<String> reservedToggleApi(String baseUrl, String docid, String userid, String otcsTicket) throws Exception {
		String endPoint = otcsApi.getReserveToggle().replace("{id}", docid);
		MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
		RequestBody body = RequestBody.create("reserved_user_id=" + userid, mediaType);

		Request request = new Request.Builder().url(baseUrl + endPoint).put(body).addHeader("otcsticket", otcsTicket)
				.addHeader("Content-Type", "application/x-www-form-urlencoded").build();

		Response response = client.newCall(request).execute();
		String responseBody = response.body() != null ? response.body().string() : "";
		if(response.isSuccessful()) {
			return ResponseEntity.ok(responseBody);
		} else {
			throw new RuntimeException(responseBody);
		}
		

//		try (Response response = client.newCall(request).execute()) {
//			String responseBody = response.body() != null ? response.body().string() : "";
//			if (!response.isSuccessful()) {
//				throw new RuntimeException("Failed to toggle reserve doc: " + response.code() + " : " + responseBody);
//			}
//			return ResponseEntity.ok(responseBody);
//		}
	}

	public ResponseEntity<String> reserveDoc(DocumentRequest requestBody) throws Exception {
		log.info("Reserving document..");
		try {
			ResponseEntity<String> response = reservedToggleApi(requestBody.getBaseUrl(), requestBody.getOtcsDocId(), requestBody.getUserId(),
					requestBody.getOtcsTicket());
			return response;
		} catch (Exception e) {
			String error = jsonObj.getJson("error", "Failed to toggle reserve doc" , e.getMessage()).toString();
			log.error(error);
			throw new RuntimeException(error);
		}
	}

	public ResponseEntity<String> reserveDoc_(DocumentRequest requestBody) {
		log.info("Reserving document..");
		ResponseEntity<String> response = callingOtcsApi.callWithRetry(() -> reservedToggleApi(requestBody.getBaseUrl(), requestBody.getOtcsDocId(),
				requestBody.getUserId(), requestBody.getOtcsTicket()), String.class);
//		log.info(response.getBody());
		return response;
	}

	public ResponseEntity<String> unreserveDoc(DocMetadata docMetadata) {
		log.info("Unreserving document..");
		try {
			ResponseEntity<String> response = reservedToggleApi(docMetadata.getBaseUrl(), docMetadata.getOtcsDocId(), null,
					docMetadata.getOtcsTicket());
			log.info(response.getBody());
			return response;
		} catch (Exception e) {
			String error = jsonObj.getJson("error", "error while unreserving doc.", e.getMessage()).toString();
			log.error(error);
			throw new RuntimeException(error);
		}
	}

	public ResponseEntity<String> unreserveDoc_(DocMetadata docMetadata) {
		log.info("Unreserving document..");
		ResponseEntity<String> response = callingOtcsApi.callWithRetry(
				() -> reservedToggleApi(docMetadata.getBaseUrl(), docMetadata.getOtcsDocId(), null, docMetadata.getOtcsTicket()), String.class);
		log.info(response.getBody());
		return response;
	}
}
