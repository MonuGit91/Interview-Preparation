package com.supai.app.otcsapis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.OtcsApi;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.otcsapis.dto.request.CreateOrCopyNodeRequestDto;
import com.supai.app.otcsapis.dto.response.CreateOrCopyNodeResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrCopyNode {
	private final static String COPY_NODE_CONTEXT = "COPY_NODE_CONTEXT";
	private final OkHttpClient okHttpClient;
	private final ObjectMapper objectMapper;
	private final OtcsApi otcsApi;
	
	private String callCopyNode(String url, Map<String, String> bodyMap, String otdsTicket) {
		log.info("Start : {}", url);
		
		MultipartBody.Builder builder = new MultipartBody.Builder();
		builder.setType(MultipartBody.FORM);
//		bodyMap.forEach((key, val) -> builder.addFormDataPart(key, val)); // or
		bodyMap.forEach(builder::addFormDataPart);
		RequestBody requestBody = builder.build();
		
		Request request = new Request.Builder()
				.url(url)
				.post(requestBody)
				.addHeader("otdsTicket", otdsTicket)
				.build();
		
		try(Response response = okHttpClient.newCall(request).execute()) {
			String body = response.body() != null ? response.body().string() : "{}";
			if(response.isSuccessful()) {
				return body;
			} else {
				log.error("{}", body);
				JsonNode errorNode = parseBody(body, url);
				throw new ExternalApiException(response.code(), errorNode, url, COPY_NODE_CONTEXT);
			}
		} catch(IOException ioExc) {
			log.error("Unexpected Error: {}", ioExc.getMessage());
			JsonNode errorNode = objectMapper.createObjectNode()
					.put("errorMsg", ioExc.getMessage())
					.put("error", ioExc.toString());
			throw new ExternalApiException(500, errorNode, url, COPY_NODE_CONTEXT);
		}	
	}
	
	public CreateOrCopyNodeResponseDto copyNode(String otdsTicket, CreateOrCopyNodeRequestDto requestDto) {
		String url = otcsApi.base.getCommon() + otcsApi.api.getCreateOrCopyNode();

		Map<String, String> requestBodyMap = objectMapper.convertValue(requestDto, new TypeReference<Map<String, String>>(){});
		String body = callCopyNode(url, requestBodyMap, otdsTicket);
		CreateOrCopyNodeResponseDto createOrCopyNodeResponseDto = jsonStrToClass(body, CreateOrCopyNodeResponseDto.class, url);
		return createOrCopyNodeResponseDto;
	}
	
	private <T> T jsonStrToClass(String body, Class<T> clazz, String url) {
		try {
			return (T) objectMapper.readValue(body, clazz);
		} catch(IOException ioExc) {
			log.error("error while mapping body to DTO - body : {}", body);
			JsonNode errorNode = objectMapper.createObjectNode()
					.put("errorBody", body)
					.put("error", ioExc.getMessage());
			throw new ExternalApiException(500, errorNode, url);
		}
	}

	private JsonNode parseBody(String body, String url) {
		// TODO Auto-generated method stub
		try {
			return objectMapper.readTree(body);
		} catch (IOException ioExc) {
			log.error("Invalid JsonBody : {}", body);
			JsonNode errorNode = objectMapper.createObjectNode().put("error", body);
			throw new ExternalApiException(500, errorNode, url);
		}
	}
}