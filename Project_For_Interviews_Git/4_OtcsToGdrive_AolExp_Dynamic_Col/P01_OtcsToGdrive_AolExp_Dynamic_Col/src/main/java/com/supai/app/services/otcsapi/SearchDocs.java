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

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.config.OtcsApi;
import com.supai.app.config.OtcsCategory;
import com.supai.app.config.OtcsCredentials;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SearchDocs {
	private static final Logger log = LoggerFactory.getLogger(SearchDocs.class);
	private final RestTemplate restTemplate;
	private final OtcsCredentials otcsCredentials;
	private final OtcsCategory otcsCategory;
	private final OtcsApi otcsApi;
	private final OtcsTicket otcsTicket;
	private final JsonObj jsonObj;
	private final CallOtcsApi callOtcsApi;

	public ResponseEntity<String> getChildlNodesProperties(String parentId) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set("otcsticket", otcsCredentials.getOtcsticket());
		headers.set("Content-Type", "application/x-www-form-urlencoded");
		HttpEntity<String> entity = new HttpEntity<>(headers);
		String url = otcsApi.getBaseUrl() + otcsApi.getChildNodeProperty().replace("{parentId}", parentId);
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		return response;
	}

	public JsonNode getChildNodes(String parentId) {
		ResponseEntity<String> response = callOtcsApi.callWithRetry(() -> getChildlNodesProperties(parentId),
				String.class);
		if (callOtcsApi.isUnExpectedResponse(response)) {
			log.info("Error: Unable to get Child Node Property  - {}", response.getBody());
			return null;
		}
		try {
//			JsonNode jsonBody = jsonObj.getJson(response.getBody());
			return jsonObj.getResultFromOtcsResponse(response);
		} catch (Exception e) {
			log.info("Faild to get the body of response", e.getMessage());
			return null;
		}

	}

	// This will run a custom search object of Content server
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

	public JsonNode getDocs() {
		// TODO Auto-generated method stub
		ResponseEntity<String> response = callOtcsApi.callWithRetry(this::searchAllDocs, String.class);
		if (callOtcsApi.isUnExpectedResponse(response)) {
			log.info("Error: Faild to call search api  - {}", response.getBody());
			return null;
		}

		try {
			return jsonObj.getResultFromOtcsResponse(response);
		} catch (Exception e) {
			log.info("Exception : Faild to get the body of search search api response - {}", e.getMessage());
			return null;
		}

	}

}
