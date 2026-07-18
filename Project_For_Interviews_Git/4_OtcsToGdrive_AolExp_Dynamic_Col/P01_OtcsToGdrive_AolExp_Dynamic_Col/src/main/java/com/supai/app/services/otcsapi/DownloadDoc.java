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

import com.supai.app.config.OtcsCredentials;
import com.supai.app.services.common.LogBuffer;
import com.supai.app.services.common.LogUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DownloadDoc {
	private static final Logger log = LoggerFactory.getLogger(DownloadDoc.class);
	private final RestTemplate restTemplate;
	private final OtcsCredentials otcsCredentials;
	private final Environment environment;
	private final CallOtcsApi callOtcsApi;

	public ResponseEntity<byte[]> downloadDocs(String id, String name) throws Exception {
		String baseUrl = environment.getProperty("otcs.api.base-url");
		String endPoint = environment.getProperty("otcs.api.get-doc");
		String url = String.format("%s%s", baseUrl, endPoint).replace("{id}", id).replace("{name}", name);

		// Create headers and add the otcsticket
		HttpHeaders headers = new HttpHeaders();
		headers.set("otcsticket", otcsCredentials.getOtcsticket());

		// Create HttpEntity with headers
		HttpEntity<String> entity = new HttpEntity<>(headers);

		// Make the GET request and get the response as a byte array
		ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

		return response;
	}

	public ResponseEntity<byte[]> GetDoc(String id, String name) {
		// TODO Auto-generated method stub
		LogBuffer.append(LogUtils.info("Downloading document from OTCS..."));
		ResponseEntity<byte[]> response = callOtcsApi.callWithRetry(()->downloadDocs(id, name), byte[].class);
		if(callOtcsApi.isUnExpectedResponse(response)) {
			LogBuffer.append(LogUtils.info("Error : Faild to download the document - {}", response.getBody().toString()));
			return null;
		}
		return response;
	}
}
