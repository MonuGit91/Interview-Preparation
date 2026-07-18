package com.supai.app.otcsapis;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supai.app.config.OtcsApi;
import com.supai.app.constants.Rock;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.services.common.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Component
@RequiredArgsConstructor
public class NodeProperty {
	private final OtcsApi otcsApi;
	private final OkHttpClient okHttpClient;
	private final JsonUtil jsonUtil;
	private Response nodePropertyApi(String url, String otcsTicket) throws IOException {
		log.info("Start : {}", url);
		Request request = new Request.Builder()
				.get()
				.url(url)
				.header(Rock.OtcsTicketKey, otcsTicket)
				.header("Accept", "application/json")
				.build();

		Response response = okHttpClient.newCall(request).execute();
		log.info("End : {}", url);
		return response;
	}
	
	public JsonNode getNodeProperty(String nodeId, String otcsTicket) {
		String url = otcsApi.base.getCommon() + otcsApi.api.getNodeProperty();
		try(Response response = nodePropertyApi(url, otcsTicket)) {
			String rawJson = (response.body() != null) ? response.body().string() : "{}";
            JsonNode body = jsonUtil.objectMapper.readTree(rawJson);

            if (response.isSuccessful()) {
                return body;
            } else {
                log.error(rawJson);
                throw new ExternalApiException(response.code(), body, "OTCS Node Property", url);
            }
		} catch(Exception e) {
			log.error("Unexpected Error : {}", e.getMessage());
            JsonNode errorNode = jsonUtil.getJson("Error", e.getMessage());
            throw new ExternalApiException(500, errorNode, "OTCS Node Property", url);
		}
	}
}
