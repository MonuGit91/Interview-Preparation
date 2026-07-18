package com.supai.app.services.otcs;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.config.OtcsApi;
import com.supai.app.config.OtcsCredentials;
import com.supai.app.dao.dto.DocMetadata;
import com.supai.app.dao.dto.DocumentRequest;
import com.supai.app.services.common.JsonObj;
import com.supai.app.services.common.LogUtils;

import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
@RequiredArgsConstructor
public class CatogeryApi {
	private static final Logger log = LoggerFactory.getLogger(CatogeryApi.class);
	private final RestTemplate restTemplate;
	private final OtcsCredentials otcsCredentials;
	private final Environment environment;
	private final OtcsApi otcsApi;
	private final JsonObj jsonObj;
	private final LogUtils logUtils;
	private final CallingOtcsApi callingOtcsApi;

	private final OkHttpClient client;

	private ResponseEntity<JsonNode> getCatogeryDetails(DocMetadata docMetadata) throws Exception {
		String baseUrl = docMetadata.getBaseUrl();
		String endPoint = otcsApi.getCategoryInfo();
		String categoryId = docMetadata.getCategoryId();
		String url = String.format("%s%s", baseUrl, endPoint).replace("{id}", docMetadata.getOtcsDocId())
				.replace("{category_id}", categoryId);

		HttpHeaders headers = new HttpHeaders();
		headers.set("otcsticket", docMetadata.getOtcsTicket());
		headers.set("Content-Type", "application/x-www-form-urlencoded");
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
		return response;
	}

	public JsonNode getCatogeryDetailsById(DocMetadata docMetadata) throws Exception {
		try {
			ResponseEntity<JsonNode> response = getCatogeryDetails(docMetadata);
			return response.getBody().path("results");
		} catch (Exception e) {
			JsonNode jsonError = jsonObj.getJson("error", "unable to get catogery details.", e.getMessage());
			log.error(jsonError.toString());
			throw new RuntimeException(jsonError.toString());
		}
	}

	public JsonNode getCatogeryDetailsById_(DocMetadata docMetadata) {
		// TODO Auto-generated method stub
		ResponseEntity<JsonNode> response = callingOtcsApi.callWithRetry(() -> getCatogeryDetails(docMetadata),
				JsonNode.class);
		if (callingOtcsApi.isUnExpectedResponse(response)) {
			log.info("Error: Unable to get catogery details  - {}", response.getBody());
			return null;
		}
		try {
			return response.getBody().path("results");// jsonObj.getResultFromOtcsResponse(response);
		} catch (Exception e) {
			log.info("Error: Unable to get catogery details from API Response");
			return null;
		}
	}

// =============================================Remove Category start====================================

	public ResponseEntity<String> removeCategory(Map<String, String> dataMap) throws Exception {
		String baseUrl = dataMap.get("baseUrl");
		String otcsDocId = dataMap.get("otcsDocId");
		String otcsTicket = dataMap.get("otcsTicket");
		String categoryId = dataMap.get("categoryId");
		
		String url = String.format("%s%s", baseUrl, otcsApi.getRemoveCategory().replace("{id}", otcsDocId)
				.replace("{category_id}", categoryId));

		MediaType mediaType = MediaType.parse("text/plain");
		RequestBody body = RequestBody.create(mediaType, "");

		Request request = new Request.Builder().url(url).method("DELETE", body).addHeader("otcsticket", otcsTicket)
				.build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new RuntimeException("Failed: HTTP " + response.code() + " - " + response.message());
			}
			return ResponseEntity.ok(response.body().string());
		}
	}

	public ResponseEntity<String> deleteCategory(Map<String, String> dataMap) throws Exception {
		try {
			ResponseEntity<String> response = removeCategory(dataMap);
			log.info("Deleted category {} for node {}", dataMap.get("categoryId"), dataMap.get("otcsDocId"));
			return response;
		} catch (Exception e) {
			String error = jsonObj.getJson("error", "error while deleting category", e.getMessage()).toString();
			log.error(error);
			throw new RuntimeException(error);
		}
	}

	public ResponseEntity<String> deleteCategory_(Map<String, String> dataMap) {
		ResponseEntity<String> response = callingOtcsApi.callWithRetry(() -> removeCategory(dataMap), String.class);
		log.info("Deleted category {} for node {}", dataMap.get("categoryId"), dataMap.get("otcsDocId"));
		return response;
	}
// =============================================Remove Category end====================================

// =============================================Add Category start==================================
	public ResponseEntity<String> addCategory(DocumentRequest userRequest, String gdriveDocId) throws Exception {
		String url = String.format("%s%s", userRequest.getBaseUrl(),
				otcsApi.getAddCategory().replace("{id}", userRequest.getOtcsDocId()));
		log.info(url);

		String formData = String.format("category_id=%s&%s=%s&%s=%s", userRequest.getCategoryId(),
				userRequest.getFeildIdOf_EditorId(), userRequest.getUserId(),
				userRequest.getFeildIdOf_GDriveDocId(), gdriveDocId);
		log.info(formData);

		MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
		RequestBody body = RequestBody.create(mediaType, formData);

		Request request = new Request.Builder().url(url).method("POST", body)
				.addHeader("otcsticket", userRequest.getOtcsTicket())
				.addHeader("Content-Type", "application/x-www-form-urlencoded").build();
		log.info(request.toString());

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				log.error(response.toString());
				throw new RuntimeException(response.toString());
			}
			return ResponseEntity.ok(response.body().string());
		}
	}

	public ResponseEntity<String> applyCategory_(DocumentRequest documentRequest, String gdriveDocId) {
		ResponseEntity<String> response = callingOtcsApi.callWithRetry(() -> addCategory(documentRequest, gdriveDocId),
				String.class);
		log.info("Added category {} for node {}", documentRequest.getCategoryId(), documentRequest.getOtcsDocId());
		return response;
	}

	public ResponseEntity<String> applyCategory(DocumentRequest documentRequest, String gdriveDocId) throws Exception {
		try {
			ResponseEntity<String> response = addCategory(documentRequest, gdriveDocId);
			log.info("Added category {} for node {}", documentRequest.getCategoryId(), documentRequest.getOtcsDocId());
			return response;
		} catch (Exception e) {
			String error = jsonObj.getJson("error", "not able to applying category", e.getMessage()).toString();
			log.error(error);
			throw new RuntimeException(error);
		}

	}

// =============================================Add Category end====================================
// =============================================Update Category Start====================================

	public ResponseEntity<String> updateCategory(Map<String, String> dataMap) {
		try {
			String baseUrl = dataMap.get("baseUrl");
			String otcsDocId = dataMap.get("otcsDocId");
			String otcsTicket = dataMap.get("otcsTicket");
			String categoryId = dataMap.get("categoryId");
			String userIdKey = dataMap.get("full_feildIdOf_EditorId");
			String gdriveDocIdKey = dataMap.get("feildIdOf_GDriveDocId");
			String url = String.format("%s%s", baseUrl, otcsApi.getUpdateCategory().replace("{id}", otcsDocId)
					.replace("{category_id}", categoryId));

			OkHttpClient client = new OkHttpClient().newBuilder().build();
			MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
			RequestBody body = RequestBody.create(mediaType, userIdKey + "=&" + gdriveDocIdKey + "=");
			Request request = new Request.Builder().url(url).method("PUT", body).addHeader("otcsticket", otcsTicket)
					.addHeader("Content-Type", "application/x-www-form-urlencoded").build();
			Response response = client.newCall(request).execute();

			return ResponseEntity.ok().body(response.body().toString());
		} catch (Exception e) {
			log.error("{}", e.getMessage());
			return null;
		}

	}

// =============================================Update Category end====================================
}
