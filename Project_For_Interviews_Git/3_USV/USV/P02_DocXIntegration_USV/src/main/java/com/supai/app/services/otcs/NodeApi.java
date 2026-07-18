package com.supai.app.services.otcs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.config.OtcsApi;
import com.supai.app.config.OtcsCredentials;
import com.supai.app.dao.dto.DocMetadata;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NodeApi {
	private static final Logger LOG = LoggerFactory.getLogger(NodeApi.class);
	private final RestTemplate restTemplate;
	private final OtcsCredentials otcsCredentials;
	private final OtcsApi otcsApi;
	private final JsonObj jsonObj;
	private final CallingOtcsApi callingOtcsApi;

	public ResponseEntity<String> callNodesProperty(DocMetadata docMetadata) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set("otcsticket", docMetadata.getOtcsTicket());
		headers.set("Content-Type", "application/x-www-form-urlencoded");
		HttpEntity<String> entity = new HttpEntity<>(headers);

		String url = docMetadata.getBaseUrl() + otcsApi.getNodeProperty().replace("{nodeId}", docMetadata.getOtcsDocId());

		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		return response;
	}

	public JsonNode getNodesProperty(DocMetadata docMetadata) throws Exception {
		try {
			ResponseEntity<String> response = callNodesProperty(docMetadata);
			return jsonObj.getResultFromOtcsResponse(response);
		} catch (Exception e) {
			String error = jsonObj.getJson("error", "Not able to get the Node Property" ,e.getMessage()).toString();
			LOG.error(error);
			throw new RuntimeException(error);
		}
	}

	public JsonNode getNodesProperty_(DocMetadata docMetadata) {
		ResponseEntity<String> response = callingOtcsApi.callWithRetry(() -> callNodesProperty(docMetadata), String.class);

		if (callingOtcsApi.isUnExpectedResponse(response)) {
			LOG.info("Error: Unable to get Node Property  - {}", response.getBody());
			return null;
		}
		try {
			return jsonObj.getResultFromOtcsResponse(response);
		} catch (Exception e) {
			LOG.error("Error: Not able to get the Node Property : {}", e.getMessage());
			return null;
		}
	}

}
