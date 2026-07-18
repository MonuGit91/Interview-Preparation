package com.supai.app.otcsapis;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.OtcsApi;
import com.supai.app.constants.Rock;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.otcsapis.dto.response.ChildNodePropertyResponseDto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Data
@Component
@RequiredArgsConstructor
public class SubNodeProperty {
	private final OkHttpClient okHttpClient;
	private final OtcsApi otcsApi;
	private final ObjectMapper objectMapper;

	private String callSubNodePropertyApi(String url, String tocsTicket) {
		log.info("Start: {}", url);
		Request request = new Request.Builder()
				.url(url)
				.get()
				.header("Accept", "application/json")
				.header(Rock.OtcsTicketKey, tocsTicket)
				.build();

		try (Response response = okHttpClient.newCall(request).execute()) {
			log.info("End: {}", url);
			String body = response.body() != null ? response.body().string() : "{}";
			if (response.isSuccessful()) {
				return body;
			} else {
				JsonNode errorNode = parseJson(body, url);
				throw new ExternalApiException(response.code(), errorNode, "Sub_Node_Property_Api", url);
			}

		} catch (IOException ioException) {
			log.error("{}", ioException.getMessage());
			JsonNode errorNode = objectMapper.createObjectNode().put("Error", ioException.getMessage());
			throw new ExternalApiException(500, errorNode, "Sub_Node_Property_Api", url);
		}

	}

	public ChildNodePropertyResponseDto getSubNodeProperty(long parentId, int pageLimit, int pageNo, String otcsTicket) {
		String url = otcsApi.base.getCommon() + otcsApi.api.getChildNodeProperty()
				.replace("{parentId}", "" + parentId)
				.replace("{pageLimit}", "" + pageLimit)
				.replace("{pageNo}", "" + pageNo);

		String responseBody = callSubNodePropertyApi(url, otcsTicket);
		JsonNode jsonBody = parseJson(responseBody, url);
		ChildNodePropertyResponseDto properties = jsonNodeToDto(jsonBody);
		return properties;
	}
	
	public ChildNodePropertyResponseDto getAllSubNodeProperty(long parentId, String otcsTicket) {;
		int pageLimit = 100;
		int currentPageNo = 1;
		
		int page_total = 1;
		ChildNodePropertyResponseDto childProperties = null;
		do {
			ChildNodePropertyResponseDto currPropertiesDto = getSubNodeProperty(parentId, pageLimit, currentPageNo, otcsTicket);
			if(childProperties == null) {
				childProperties = currPropertiesDto;
			} else {
				childProperties.getResults().addAll(currPropertiesDto.getResults());
			}
			page_total = currPropertiesDto.getCollection().getPaging().getPage_total();
		} while(++currentPageNo <= page_total);
		
		return childProperties;
	}
	
	
	//============================== Utilities ========================================
	private ChildNodePropertyResponseDto jsonNodeToDto(JsonNode jsonResponseBody) {
		try {
			return objectMapper.treeToValue(jsonResponseBody, ChildNodePropertyResponseDto.class);
		} catch (Exception e) {
			log.error("Error while mapping subNodeProperty to DTO: {}", e.getMessage());
			throw new RuntimeException("Error while mapping subNodeProperty to DTO", e);
		}
	}

	private JsonNode parseJson(String body, String url) {
		try {
			return objectMapper.readTree(body);
		} catch (Exception e) {
			log.error("{}", e.getMessage());
			JsonNode errorNode = objectMapper.createObjectNode()
					.put("error", body);
			throw new ExternalApiException(500, errorNode, "Sub_Node_Property_Api", url);
		}
	}
}
