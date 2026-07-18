package com.supai.app.otds;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.OtdsConfig;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.otcsapis.dto.response.OtdsTokenResponseDto;
import com.supai.app.services.common.JsonUtil;

import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImpersonateUser {

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final OtdsConfig otdsConfig;
    private final JsonUtil jsonUtil;
    private final OtdsToken otdsToken;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private Response authorize(String userName, String ticket, String url) throws IOException {
        log.info("Start : {}", url);
        // 1. Create a Map for the request body to avoid manual string escaping
        Map<String, String> requestData = new HashMap<>();
        requestData.put("userName", userName);
        requestData.put("ticket", ticket);

        // 2. Convert Map to JSON String
        String jsonBody = objectMapper.writeValueAsString(requestData);
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);

        // 3. Build the Request with the Bearer Token
        Request request = new Request.Builder()
                .url(url) // URL from your config
                .post(body)
                .addHeader("Accept", "application/json")
                .build();

        Response response = client.newCall(request).execute();
        log.info("End : {}", url);
        return response;
    }

    //authorizeUser
    public JsonNode getOtdsTicketByUserName(String userName) {
		OtdsTokenResponseDto otdsTokenResponseDto = otdsToken.getOtdsToken();
		String otdsTicket = otdsTokenResponseDto.getTicket();
        String url = otdsConfig.getApi().getImpersonateUser();

        try (Response response = authorize(userName, otdsTicket, url)) {

            String bodyString = (response.body() != null) ? response.body().string() : "{}";
            JsonNode jsonNode = objectMapper.readTree(bodyString);

            if (response.isSuccessful()) {
                return jsonNode;
            } else {
                throw new ExternalApiException(response.code(), jsonNode, "OTDS Impersonate User", url);
            }
        } catch (IOException e) {
            log.error("Failed to impersonate user: {}", e.getMessage());
            JsonNode errorNode = jsonUtil.getJson("Error", e.getMessage());
            throw new ExternalApiException(502, errorNode, "OTDS Impersonate User", url);
        }
    }

    public String getOtdsTicket() {
    	JsonNode impersonatedUserOtdsTicketJson = getOtdsTicketByUserName(otdsConfig.getUserId()).get("ticket");
    	String impersonatedUserOtdsTicket = impersonatedUserOtdsTicketJson.asText();
        log.info("otdsTicket: {}", impersonatedUserOtdsTicket);
        
        return impersonatedUserOtdsTicket;
    }
}