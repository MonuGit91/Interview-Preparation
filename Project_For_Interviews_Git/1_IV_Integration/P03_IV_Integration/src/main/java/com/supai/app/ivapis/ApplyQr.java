package com.supai.app.ivapis;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.IvConfig;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.services.common.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplyQr {
	private final OkHttpClient client;
	private final ObjectMapper mapper;
	private final JsonUtil jsonUtil;
	private final IvConfig ivConfig;
	private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

	private Response attachImage(String jsonBody, String bearerToken) throws IOException {
		log.info("Start : {}", ivConfig.getApi().getGraphQl());
		RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
		Request request = new Request.Builder().url(ivConfig.getApi().getGraphQl()).post(body)
				.addHeader("Authorization", "Bearer " + bearerToken).build();
		Response response = client.newCall(request).execute();
		log.info("End : {}", ivConfig.getApi().getGraphQl());
		return response;
	}

	private Response graphQlFollowUp(String otdsTicket, String nodeId, int versionNo) throws IOException {
		String url = ivConfig.getApi().getGraphQlFollowup().replace("{nodeId}", "" + nodeId).replace("{versionNo}",
				"" + versionNo);
		log.info("Start : {}", url);
		String jsonBody = "\r\n{\"text\":{\"basic/public\":\" \"}}";
		RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
		Request request = new Request.Builder().url(url).post(body).addHeader("otdsTicket", otdsTicket)
				.addHeader("Content-Type", "application/json").build();
		Response response = client.newCall(request).execute();
		log.info("End : {}", url);
		return response;
	}

	public JsonNode applyQrCodeFollowUp(String otdsTicket, String nodeId, int versionNo) {
		try (Response response = graphQlFollowUp(otdsTicket, nodeId, versionNo)) {
			if (!response.isSuccessful()) {
				String errorDetails = response.body() != null ? response.body().string() : "{}";
				JsonNode errorNode;
				try {
					errorNode = mapper.readTree(errorDetails);
				} catch (Exception e) {
					errorNode = mapper.createObjectNode().put("message", errorDetails);
				}
				throw new ExternalApiException(response.code(), errorNode, "QR Attachment",
						ivConfig.getApi().getGraphQlFollowup());
			}

			return mapper.readTree(response.body().string());
		} catch (Exception e) {
			JsonNode errorNode = jsonUtil.getJson("message", e.getMessage());
			throw new ExternalApiException(500, errorNode, "QR Attachment", ivConfig.getApi().getGraphQlFollowup());
		}
	}

	public JsonNode applyQrCode(String jsonBody, String bearerToken) {
		try (Response response = attachImage(jsonBody, bearerToken)) {
			if (!response.isSuccessful()) {
				String errorDetails = response.body() != null ? response.body().string() : "{}";
				JsonNode errorNode;
				try {
					errorNode = mapper.readTree(errorDetails);
				} catch (Exception e) {
					errorNode = mapper.createObjectNode().put("message", errorDetails);
				}
				throw new ExternalApiException(response.code(), errorNode, "QR Attachment",
						ivConfig.getApi().getGraphQl());
			}

			return mapper.readTree(response.body().string());
		} catch (Exception e) {
			JsonNode errorNode = jsonUtil.getJson("message", e.getMessage());
			throw new ExternalApiException(500, errorNode, "QR Attachment", ivConfig.getApi().getGraphQl());
		}
	}

}
