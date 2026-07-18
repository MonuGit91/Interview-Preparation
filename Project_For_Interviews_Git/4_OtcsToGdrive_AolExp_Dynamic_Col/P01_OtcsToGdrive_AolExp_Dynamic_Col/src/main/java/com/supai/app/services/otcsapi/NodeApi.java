package com.supai.app.services.otcsapi;




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
import com.supai.app.config.OtcsCategory;
import com.supai.app.config.OtcsCredentials;
import com.supai.app.services.common.JsonObj;
import com.supai.app.services.common.LogBuffer;
import com.supai.app.services.common.LogUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NodeApi {
	private static final Logger LOG = LoggerFactory.getLogger(NodeApi.class);
	private final RestTemplate restTemplate;
	private final OtcsCredentials otcsCredentials;
	private final OtcsCategory otcsCategory;
	private final OtcsApi otcsApi;
	private final OtcsTicket otcsTicket;
	private final JsonObj jsonObj;
	private final CallOtcsApi callOtcsApi;
	
	public ResponseEntity<String> callNodesProperty(String nodeId)  throws Exception  {
		HttpHeaders headers = new HttpHeaders();
		headers.set("otcsticket", otcsCredentials.getOtcsticket());
		headers.set("Content-Type", "application/x-www-form-urlencoded");
		HttpEntity<String> entity = new HttpEntity<>(headers);
		
		String url =  otcsApi.getBaseUrl() + otcsApi.getNodeProperty().replace("{nodeId}", nodeId);
		
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

		return response;
	}
	
	public JsonNode getNodesProperty(String nodeId) { 
		ResponseEntity<String> response = callOtcsApi.callWithRetry(() -> callNodesProperty(nodeId), String.class);
		if(callOtcsApi.isUnExpectedResponse(response)) {
			LogBuffer.append(LogUtils.info("Error: Unable to get Node Property  - {}", response.getBody()));
			return null;
		}
		try {
		    return jsonObj.getResultFromOtcsResponse(response); 
		} catch (Exception e) {
			LogBuffer.append(LogUtils.info("Error: Not able to get the Node Property : {}", e.getMessage()));
		    return null;
		}
	}

}
