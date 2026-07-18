package com.supai.app.services.otcs;

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
import com.supai.app.config.OtcsCredentials;
import com.supai.app.dao.dto.DocumentRequest;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DownloadDoc {
	private static final Logger log = LoggerFactory.getLogger(DownloadDoc.class);
	private final RestTemplate restTemplate;
	private final OtcsCredentials otcsCredentials;
	private final Environment environment;
	private final CallingOtcsApi callingOtcsApi;
	private final JsonObj jsonObj;

	private ResponseEntity<byte[]> downloadDocs(String baseUrl, String otcsDocId, String name, String otcsTicket) throws Exception {
		String endPoint = environment.getProperty("otcs.api.get-doc");
		String url = String.format("%s%s", baseUrl, endPoint).replace("{id}", otcsDocId).replace("{name}", name);

		// Create headers and add the otcsticket
		HttpHeaders headers = new HttpHeaders();
		headers.set("otcsticket", otcsTicket);

		// Create HttpEntity with headers
		HttpEntity<String> entity = new HttpEntity<>(headers);

		// Make the GET request and get the response as a byte array
		ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

		return response;
	}
	
	public ResponseEntity<byte[]> GetDoc(DocumentRequest request, String name) throws Exception {
		// TODO Auto-generated method stub
		log.info("Downloading document from OTCS...");
		ResponseEntity<byte[]> response = downloadDocs(request.getBaseUrl(), request.getOtcsDocId(), name, request.getOtcsTicket());
		if(callingOtcsApi.isUnExpectedResponse(response)) {
			String error = jsonObj.getJson("error", "Faild to download the document", response.getBody().toString()).toString();
			log.error(error);
			throw new RuntimeException(error);
		}
		return response;
	}
	
	public ResponseEntity<byte[]> GetDoc_(DocumentRequest request, String name) {
		// TODO Auto-generated method stub
		log.info("Downloading document from OTCS...");
		ResponseEntity<byte[]> response = callingOtcsApi.callWithRetry(()->downloadDocs(request.getBaseUrl(),request.getOtcsDocId(), name, request.getOtcsTicket()), byte[].class);
		if(callingOtcsApi.isUnExpectedResponse(response)) {
			log.error("Faild to download the document - {}", response.getBody());
			return null;
		}
		return response;
	}
}
