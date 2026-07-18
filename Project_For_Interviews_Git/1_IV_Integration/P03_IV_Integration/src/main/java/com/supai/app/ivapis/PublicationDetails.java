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
public class PublicationDetails {

    private final OkHttpClient client;
    private final IvConfig ivConfig;
    private final JsonUtil jsonUtil;

    public JsonNode callPageDetails(IvTicketResponse ivTicketResponse) {
        String url = ivConfig.getApi().getStatus().replace("{publicationId}",
                ivTicketResponse.getValidNodes().get(0).getPubId())
                + "?embed=page_links";

        log.info("Start : {}", url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + ivTicketResponse.getToken())
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            log.info("End : {}", url);
            if (response.body() == null) {
                JsonNode errorNode = jsonUtil.getJson("Error", "Empty response from server");
                throw new ExternalApiException(502, errorNode, "IV Page Details", url);
            }
            
            String responseBody = response.body().string();
            JsonNode bodyNode = jsonUtil.getJson(responseBody);
            
            log.info("{}", bodyNode.toString());
            
            if (!response.isSuccessful()) {
                if (responseBody == null) // Check if responseBody is null/empty if handled differently above
                    bodyNode = jsonUtil.getJson("Error", "Unknown error");
                // Re-parsing logic from PublicationStatus for consistency
                if (bodyNode == null)
                    bodyNode = jsonUtil.getJson("Error", responseBody);

                throw new ExternalApiException(response.code(), bodyNode, "IV Page Details", url);
            }
            
            String status = bodyNode.get("status").asText();
            if (response.isSuccessful() && status.equals("Failed")) {
                if (responseBody == null) // Check if responseBody is null/empty if handled differently above
                    bodyNode = jsonUtil.getJson("Error", "Unknown error");
                // Re-parsing logic from PublicationStatus for consistency
                if (bodyNode == null)
                    bodyNode = jsonUtil.getJson("Error", responseBody);

                throw new ExternalApiException(401, bodyNode, "IV Page Details", url);
            }

            return bodyNode;
        } catch (IOException e) {
            log.error("Failed callPublicationDetails: {}", e.getMessage());
            JsonNode errorNode = jsonUtil.getJson("Error", e.getMessage());
            throw new ExternalApiException(502, errorNode, "IV Page Details", url);
        }
    }
}
