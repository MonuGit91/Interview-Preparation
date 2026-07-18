package com.supai.app.otcsapis;

import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.config.OtcsApi;
import com.supai.app.constants.Rock;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.services.common.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
@RequiredArgsConstructor
@Slf4j
public class AllVersions {

    // Reuse the client to maintain connection pooling
//    @Value("${ticketType}")
//    private String ticketType;
    private final OkHttpClient okHttpClient;
    private final JsonUtil jsonUtil;
    private final OtcsApi otcsApi;

    public Response nodeVersionsApi(String url, String otcsTicket) throws Exception {
        log.info("Start : {}", url);
//        Thread.sleep(1000 * 15);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader(Rock.OtcsTicketKey, otcsTicket)
                .addHeader("Accept", "application/json")
                .build();

        // 2. Executing the call and returning the complete response
        Response response = okHttpClient.newCall(request).execute();
        log.info("End : {}", url);
        return response;
    }

    public JsonNode getNodeVersions(String nodeId, String otcsTicket) {
        String url = otcsApi.base.getCommon() + otcsApi.api.getAddOrGetVersion().replace("{nodeId}", nodeId);
        try (Response response = nodeVersionsApi(url, otcsTicket)) {

            String rawJson = (response.body() != null) ? response.body().string() : "{}";
            JsonNode body = jsonUtil.objectMapper.readTree(rawJson);

            if (response.isSuccessful()) {
                return body; // Success path
            } else {
                log.error(rawJson);
                throw new ExternalApiException(response.code(), body, "OTCS Get All Versions", url);
            }

        } catch (Exception e) {
            // If the server is down or network fails
            log.error("Network error: {}", e.getMessage());
            // You can throw a standard RuntimeException or a custom one
            JsonNode errorNode = jsonUtil.getJson("Error", e.getMessage());
            throw new ExternalApiException(502, errorNode, "OTCS Get All Versions", url);
        }
    }

    public int getMaxVersion(String nodeId, String otcsTicket) {
        JsonNode fullResponse = getNodeVersions(nodeId, otcsTicket);
        JsonNode dataNode = fullResponse.get("data");

        // Instead of returning -1, throw a specific error if the structure is wrong
        if (dataNode == null || !dataNode.isArray()) {
            throw new RuntimeException("Invalid response structure: 'data' field missing or not an array");
        }

        int maxVersion = StreamSupport.stream(dataNode.spliterator(), false)
                .map(node -> node.get("version_number"))
                .filter(v -> v != null && v.isInt())
                .mapToInt(JsonNode::asInt)
                .max()
                .orElse(-1);
        log.info("max version: {}", maxVersion);
        return maxVersion;
    }
    
    public int extractMaxVersion(JsonNode versionResponse) {
    	JsonNode dataNode = versionResponse.get("data");

        // Instead of returning -1, throw a specific error if the structure is wrong
        if (dataNode == null || !dataNode.isArray()) {
            throw new RuntimeException("Invalid response structure: 'data' field missing or not an array");
        }

        int maxVersion = StreamSupport.stream(dataNode.spliterator(), false)
                .map(node -> node.get("version_number"))
                .filter(v -> v != null && v.isInt())
                .mapToInt(JsonNode::asInt)
                .max()
                .orElse(-1);
        log.info("max version: {}", maxVersion);
        return maxVersion;
    }
}