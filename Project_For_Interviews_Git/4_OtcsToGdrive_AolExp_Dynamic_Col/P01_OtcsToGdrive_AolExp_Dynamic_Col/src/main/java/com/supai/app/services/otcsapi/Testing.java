package com.supai.app.services.otcsapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.supai.app.config.OtcsApi;
import com.supai.app.config.OtcsCategory;
import com.supai.app.config.OtcsCredentials;
import com.supai.app.services.common.LogUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class Testing {
	private static final Logger log = LoggerFactory.getLogger(Testing.class);
	private final RestTemplate restTemplate;
	private final OtcsCredentials otcsCredentials;
	private final OtcsCategory otcsCategory;
	private final OtcsApi otcsApi;
	
	public ResponseEntity<String> searchAllDocs() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set("otcsticket", otcsCredentials.getOtcsticket());
		headers.set("Content-Type", "application/x-www-form-urlencoded");

		// Set body (form data)
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("query_id", otcsCategory.getQueryId());

		// Create HttpEntity with headers and body
		HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

		// Make POST request
		String url = otcsApi.getBaseUrl() + otcsApi.getSearchDocs();
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
		return response;
	}

	public void getDocs() {
		// TODO Auto-generated method stub
		ResponseEntity<String> response = null;
		System.out.println(1);
		try {
			// Attempt the provided action and log success message if provided
			System.out.println(2);
			response = searchAllDocs();
		} catch (Exception e) {
			return;
		}
		
		try {
			String body = response.getBody();
			System.out.println(body);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
	}
}
