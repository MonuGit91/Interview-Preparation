package com.supai.app.ivapis;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class Merge {

    private final OkHttpClient okHttpClient;
    private final JsonUtil jsonUtil;
    private final IvConfig ivConfig;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /**
     * Low-level API call to ViewX Transform
     */
    private Response transformNodesApi(String url, String otdsTicket, String jsonBody) throws IOException {
        log.info("Start : {}", url);
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("otdsTicket", otdsTicket)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        Response response = okHttpClient.newCall(request).execute();
        log.info("End : {}", url);
        return response;
    }

    /**
     * High-level method to merge/transform nodes and handle the response
     */
    public JsonNode transformNodes(String otdsTicket, String jsonBody) {
    	 String url = ivConfig.getApi().getMerge();
    	 
        try (Response response = transformNodesApi(url, otdsTicket, jsonBody)) {
            log.info("Transform API Status Code: {}", response.code());
            if (response.body() != null) {
                String rawJson = response.body().string();
                ObjectNode body = (ObjectNode) jsonUtil.objectMapper.readTree(rawJson);
                if (response.isSuccessful()) {
                    return body;
                } else {
                    log.error("API Error: {}", rawJson);
                    JsonNode errorNode = body;
                    throw new ExternalApiException(502, errorNode, "Merge Document", url);
                }
            } else {
            	JsonNode errorNode = jsonUtil.getJson("Error", "Empty response body from transform API");
            	throw new ExternalApiException(402, errorNode, "Merge Document", url);
            }
        } catch (IOException e) {
            log.error("Connection error during transform: {}", e.getMessage());
            JsonNode errorNode = jsonUtil.getJson("Error", e.getMessage());
            throw new ExternalApiException(402, errorNode, "Merge Document", url);
        }
    }
}