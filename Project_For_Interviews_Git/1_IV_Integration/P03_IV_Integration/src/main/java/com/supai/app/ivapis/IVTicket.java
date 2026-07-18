package com.supai.app.ivapis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.IvConfig;

import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.ivapis.dto.json.PubBody;
import com.supai.app.ivapis.dto.response.IvTicketResponse;
import com.supai.app.services.common.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
@Slf4j
@RequiredArgsConstructor
public class IVTicket {
    @Value("${ticketType}")
    private String ticketType;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IvConfig ivConfig;
    private final JsonUtil jsonUtil;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .cookieJar(new CookieJar() {
                private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    cookieStore.put(url.host(), cookies);
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> cookies = cookieStore.get(url.host());
                    return cookies != null ? cookies : new ArrayList<>();
                }
            })
            .build();

    public Response pubApi(String url, String ticket, String nodeId, String pubBody) throws IOException { // Changed
                                                                                                          // signature
        // Prepare the payload

        // String jsonBody =
        // String.format("{\"nodes\":[{\"id\":%s,\"vernum\":1,\"vertype\":\"\"}]}",
        // nodeId);
        log.info("Start : {}", url);
        RequestBody body = new FormBody.Builder()
                .add("body", pubBody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader(ticketType, ticket)
                // NO COOKIE HEADER NEEDED HERE anymore! The CookieJar handles it.
                .build();
        Response response = client.newCall(request).execute();
        log.info("End : {}", url);
        return response;
    }

    public IvTicketResponse callPubApi(String baseUrl, String ticket, String nodeId, int maxVersionNo) {
        String url = baseUrl + ivConfig.getApi().getPub();
        PubBody pubBodyObj = PubBody.getBody(Long.valueOf(nodeId), maxVersionNo);
        String pubBody = jsonUtil.classToJson(pubBodyObj);
        try (Response response = pubApi(url, ticket, nodeId, pubBody)) {
            // Read body once
            String bodyString = response.body() != null ? response.body().string() : "{}";

            if (!response.isSuccessful()) {
                JsonNode errorNode = jsonUtil.getJson(bodyString);
                if (errorNode == null)
                    errorNode = jsonUtil.getJson("Error", bodyString);
                throw new ExternalApiException(response.code(), errorNode, "IV Ticket Generation", url);
            }

            IvTicketResponse ivTicketResponse = jsonUtil.jsonToDto(bodyString, IvTicketResponse.class);

            log.info("Bearer Token: {}" + ivTicketResponse.getToken());
            return ivTicketResponse;

        } catch (IOException e) {
            log.error("Failed callPubApi: {}", e.getMessage());
            JsonNode errorNode = jsonUtil.getJson("Error", e.getMessage());
            throw new ExternalApiException(502, errorNode, "IV Ticket Generation", url);
        }
    }
}
