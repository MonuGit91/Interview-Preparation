package com.supai.app.otcsapis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.OtcsApi;
import com.supai.app.config.OtdsConfig;
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
public class OtcsToken {
	private final OkHttpClient client;
	private final ObjectMapper objectMapper;
	private final JsonUtil jsonUtil;
	private final OtdsConfig otdsConfig;
	private final OtcsApi otcsApi;
	private final static String API_CONTEXT = "OTCS_Ticket";
	private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

	private String authenticate(String userName, String password, String url){
		log.info("Start : {}", url);
		
		String jsonBody = objectMapper.createObjectNode()
				.put("userName", userName)
				.put("password", password).toString();

		RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.addHeader("Accept", "application/json")
				.build();

		try (Response response = client.newCall(request).execute()) {
			log.info("End: {}", url);
			String bodyStr = response.body() != null ? response.body().string() : "{}";
			if (response.isSuccessful()) {
				return bodyStr;
			} else {
				JsonNode errorNode = parseJson(bodyStr, url);
				throw new ExternalApiException(response.code(), errorNode, API_CONTEXT, url);
			}

		} catch (IOException ioException) {
			log.error("{}", ioException.getMessage());
			JsonNode errorNode = objectMapper.createObjectNode().put("Error", ioException.getMessage());
			throw new ExternalApiException(500, errorNode, API_CONTEXT, url);
		}
	}

	public JsonNode getOtcsTicketJson() {
		String url = otcsApi.base.getCommon() + otcsApi.api.getAuthApi();
		String jsonBodyString = authenticate(otdsConfig.getUserId(), otdsConfig.getPassword(), url);
		
		JsonNode jsonNode = parseJson(jsonBodyString, url);
		log.info("OtcsTicket: {}", jsonNode.at("/ticket").asText());
		
		return jsonNode;
	}
	
	private JsonNode parseJson(String jsonString, String url) {
		try {
			return objectMapper.readTree(jsonString);
		} catch (Exception e) {
			log.error("{}", e.getMessage());
			if (jsonString.equals("{}")) jsonString = "Response body is either empty or invalid.";
			JsonNode errorNode = objectMapper.createObjectNode()
					.put("msg", jsonString)
					.put("error", jsonString);
			throw new ExternalApiException(500, errorNode, API_CONTEXT, url);
		}
	}

}
