package com.supai.app.otcsapis;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supai.app.config.OtcsApi;
import com.supai.app.constants.Rock;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.services.common.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddVersion {
	private final OkHttpClient OkHttpClient;
	private final OtcsApi otcsApi;
	private final JsonUtil jsonUtil;

	private String uploadNewVersion(String url, String otdsTicket, byte[] fileData, String fileName)
			throws IOException { // Changed signature

		// 1. Create the File part of the request from the byte[]
		// We use "application/octet-stream" for raw binary data
		RequestBody fileBody = RequestBody.create(fileData, MediaType.parse("application/octet-stream"));

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("file", fileName + ".pdf", fileBody)
				.build();

		log.info("Start : {}", url);
		Request request = new Request.Builder()
				.url(url)
				.post(requestBody)
				.addHeader(Rock.OtdsTicketKey, otdsTicket)
				.build();

		// 4. Execute synchronously
		try (Response response = OkHttpClient.newCall(request).execute()) {
			log.info("End : {}", url);
			ResponseBody responseBody = response.body();
			String result = (responseBody != null) ? responseBody.string() : "";

			if (!response.isSuccessful()) {
				// Parse error JSON if possible
				JsonNode errorNode = jsonUtil.getJson(result);
				if (errorNode == null)
					errorNode = jsonUtil.getJson("Error", result);
				throw new ExternalApiException(response.code(), errorNode, "OTCS Add Version", url);
			}

			return result;
		}
	}

	public JsonNode addVersion(String baseUrl, String nodeId, String otdsTicket, byte[] fileData, String fileName) {
		String url = baseUrl + otcsApi.getApi().getAddOrGetVersion().replace("{nodeId}", nodeId);
		try {
			String response = uploadNewVersion(url, otdsTicket, fileData, fileName);
			ObjectNode node = (ObjectNode) jsonUtil.objectMapper.readTree(response);
			node.put("success", true);
			return node;
		} catch (IOException e) {
			log.error("Failed addVersion: {}", e.getMessage());
			JsonNode errorNode = jsonUtil.getJson("Error", e.getMessage());
			throw new ExternalApiException(502, errorNode, "OTCS Add Version", url);
		}
	}

}