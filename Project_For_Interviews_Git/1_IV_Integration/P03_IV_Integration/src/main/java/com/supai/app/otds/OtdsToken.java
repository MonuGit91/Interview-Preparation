package com.supai.app.otds;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.OAuthConfig;
import com.supai.app.config.OtdsConfig;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.otcsapis.dto.response.OtdsTokenResponseDto;
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
public class OtdsToken {

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final OtdsConfig otdsConfig;
    private final OAuthConfig oAuthConfig;
    private final JsonUtil jsonUtil;
    public static final String OTDS_Ticket_Context = "OTDS_Ticket_Context";

    // Set as a constant for efficiency
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private String callOtdsTicketApi(String userName, String password, String url){
        log.info("Start : {}", url);
        String jsonBody = jsonUtil.objectMapper.createObjectNode()
        		.put("userName", userName)
        		.put("password", password).toString();

        RequestBody requestBody = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .build();

        try(Response response = client.newCall(request).execute()) {
        	String body = response.body() != null ? response.body().string() : "{}";
        	if(response.isSuccessful()) {
        		return body;
        	} else {
        		log.error("{}", body);
        		JsonNode errorNode = parseJson(body, url);
        		throw new ExternalApiException(response.code(), errorNode, OTDS_Ticket_Context);
        	}
        } catch(IOException e) {
        	log.error("{}", e.getMessage());
        	JsonNode errorNode = jsonUtil.objectMapper.createObjectNode().put("error", e.getMessage());
        	throw new ExternalApiException(500, errorNode, "OTDS_Ticket", url);
        }
    }


	public OtdsTokenResponseDto getOtdsToken() {
        String url = otdsConfig.getApi().getAuthenticate();
        String body = callOtdsTicketApi(otdsConfig.getUserId(), otdsConfig.getPassword(), url);
//        String body = callOtdsTicketApi(oAuthConfig.getClientId(), oAuthConfig.getClientSecret(), url);

        OtdsTokenResponseDto otdsToken = mapToDto(body, url);
        return otdsToken;
    }
	
	private OtdsTokenResponseDto mapToDto(String jsonString, String url) {
	        try {
				return objectMapper.readValue(jsonString, OtdsTokenResponseDto.class);
			} catch (JsonProcessingException e) {
				log.error("Error while mapping String to DTO. Exception - {}", e.getMessage());
				JsonNode error = objectMapper.createObjectNode()
				.put("msg", "Error while mapping String to DTO. Exception - {}")
				.put("error", e.getMessage());
				throw new ExternalApiException(500, error, OTDS_Ticket_Context, url);
			}
	}
	
    private JsonNode parseJson(String str, String url) {
		// TODO Auto-generated method stub
    	try {
    		JsonNode jsonNode = objectMapper.readTree(str);
    		return jsonNode;
    	} catch (IOException ioExc) {
    		log.error("{}", str);
    		JsonNode errorNode = objectMapper.createObjectNode().put("error", str);
    		throw new ExternalApiException(500, errorNode, OTDS_Ticket_Context, url);
    	}
	}
}