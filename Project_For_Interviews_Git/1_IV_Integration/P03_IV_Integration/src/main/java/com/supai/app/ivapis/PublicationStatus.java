package com.supai.app.ivapis;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.config.IvConfig;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.ivapis.dto.response.IvTicketResponse;
import com.supai.app.services.common.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
@RequiredArgsConstructor
@Slf4j
public class PublicationStatus {

    // It's best practice to reuse a single OkHttpClient instance
    private final OkHttpClient client; // = new OkHttpClient().newBuilder().build();
    private final IvConfig ivConfig;
    private final JsonUtil jsonUtil;

    private Response getPublicationStatus(String publicationId, IvTicketResponse ivTicketResponse, String url)
            throws IOException { // Changed
        // signature
        log.info("Start : {}", url);
        Request request = new Request.Builder()
                .url(url)
                .get() // Standard GET request
                .addHeader("Authorization", "Bearer " + ivTicketResponse.getToken())
                .addHeader("Accept", "application/json") // Good practice to tell server you want JSON
                .build();

        Response response = client.newCall(request).execute();
        log.info("End : {}", url);
        return response;

        // // Use try-with-resources to ensure the response body is closed properly
        // try (Response response = client.newCall(request).execute()) {
        // if (response.body() == null) {
        // // null body logic
        // JsonNode errorNode = jsonUtil.getJson("Error", "Empty response from server");
        // throw new ExternalApiException(502, errorNode, "IV Publication Status", url);
        // }
        //
        // String responseData = response.body().string();
        //
        // if (!response.isSuccessful()) {
        // JsonNode errorNode = jsonUtil.getJson(responseData);
        // if (errorNode == null)
        // errorNode = jsonUtil.getJson("Error", responseData);
        // throw new ExternalApiException(response.code(), errorNode, "IV Publication
        // Status", url);
        // }
        // return responseData;
        // }
    }

    public JsonNode callPublicationStatus(String publicationId, IvTicketResponse ivTicketResponse) {
        String url = ivConfig.getApi().getStatus().replace("{publicationId}", publicationId);

        try (Response response = getPublicationStatus(publicationId, ivTicketResponse, url)) {
            if (response.body() == null) {
                // null body logic
                JsonNode errorNode = jsonUtil.getJson("Error", "Empty response from server");
                throw new ExternalApiException(502, errorNode, "IV Publication Status", url);
            }

            String responseBody = response.body().string();
            JsonNode bodyNode = jsonUtil.getJson(responseBody);

            if (!response.isSuccessful() || bodyNode.get("status").asText().equals("Failed")) {
                if (responseBody == null)
                    bodyNode = jsonUtil.getJson("Error", responseBody);
                throw new ExternalApiException(400, bodyNode, "IV Publication Status", url);
            }

            return bodyNode;
        } catch (IOException e) {
            log.error("Failed callPublicationStatus: {}", e.getMessage());
            JsonNode errorNode = jsonUtil.getJson("Error", e.getMessage());
            throw new ExternalApiException(502, errorNode, "IV Publication Status", url);
        }

    }
}