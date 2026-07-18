package com.supai.app.otcsapis;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supai.app.config.OtcsApi;
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
public class UploadToCS {
	@Value("${ticketType}")
	private String ticketType;

	private final OkHttpClient okHttpClient;
	private final JsonUtil jsonUtil;
	private final OtcsApi otcsApi;

	private String uploadDoc(String url, String otcsTicket, byte[] fileData, String fileName, String nodeId)
			throws IOException {

		// 1. Create the File part
		RequestBody fileBody = RequestBody.create(fileData, MediaType.parse("application/octet-stream"));

		// 2. Create the metadata body part (as seen in Postman snippet)
		// type 144 = Document, parent_id = nodeId
		String bodyContent = String.format("{\"type\":144, \"parent_id\":%s, \"name\":\"%s\"}", nodeId, fileName);

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("file", fileName, fileBody)
				.addFormDataPart("body", bodyContent)
				.build();

		log.info("Start : {}", url);
		Request request = new Request.Builder()
				.url(url)
				.post(requestBody)
				.addHeader("OtcsTicket", otcsTicket)
				.build();

		// 3. Execute synchronously
		try (Response response = okHttpClient.newCall(request).execute()) {
			log.info("End : {}", url);
			ResponseBody responseBody = response.body();
			String result = (responseBody != null) ? responseBody.string() : "";

			if (!response.isSuccessful()) {
				JsonNode errorNode = jsonUtil.getJson(result);
				if (errorNode == null)
					errorNode = jsonUtil.getJson("Error", result);
				throw new ExternalApiException(response.code(), errorNode, "OTCS Upload", url);
			}

			return result;
		}
	}

	public JsonNode uploadToCS(String parentNodeId, String otcsTicket, byte[] fileData, String fileName) {
		String url = otcsApi.base.getCommon() + otcsApi.api.getCreateOrCopyNode();
		try {
			String response = uploadDoc(url, otcsTicket, fileData, fileName, parentNodeId);
			ObjectNode node = (ObjectNode) jsonUtil.objectMapper.readTree(response);
			node.put("success", true);
			return node;
		} catch (IOException e) {
			log.error("Failed uploadToCS: {}", e.getMessage());
			JsonNode errorNode = jsonUtil.getJson("Error", e.getMessage());
			throw new ExternalApiException(502, errorNode, "OTCS Upload", url);
		}
	}

}