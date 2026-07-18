package com.supai.app.services.otcsapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.config.OtcsApi;
import com.supai.app.config.OtcsCredentials;
import com.supai.app.services.common.JsonObj;
import com.supai.app.services.common.LogBuffer;
import com.supai.app.services.common.LogUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CatogeryApi {
	private static final Logger log = LoggerFactory.getLogger(CatogeryApi.class);
	private final RestTemplate restTemplate;
	private final OtcsCredentials otcsCredentials;
	private final Environment environment;
	private final OtcsTicket otcsTicket;
	private final OtcsApi otcsApi;
	private final JsonObj jsonObj;
	private final LogUtils logUtils;
	private final CallOtcsApi callOtcsApi;
	
	private ResponseEntity<String> getCatogeryDetails(String docId) throws Exception {
		String baseUrl = otcsApi.getBaseUrl();
		String endPoint = otcsApi.getCategoryInfo();
		String categoryId = environment.getProperty("otcs.category.category-id");
		String url = String.format("%s%s", baseUrl, endPoint).replace("{id}", docId).replace("{category_id}", categoryId);
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("otcsticket", otcsCredentials.getOtcsticket());
		headers.set("Content-Type", "application/x-www-form-urlencoded");
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		return response;
	}
	public JsonNode getCatogeryDetailsById(String docId) {
		// TODO Auto-generated method stub
		try {
			ResponseEntity<String> response = callOtcsApi.callWithRetry(() -> getCatogeryDetails(docId), String.class);
			if (callOtcsApi.isUnExpectedResponse(response)) {
				LogBuffer.append(LogUtils.info("Error: Unable to get catogery details  - {}", response.getBody()));
				return null;
			}
			return jsonObj.getResultFromOtcsResponse(response);
		} catch (Exception e) {
			LogBuffer.append(LogUtils.info("Error: Unable to get catogery details from API Response"));
			return null;
		} 
	}
}
