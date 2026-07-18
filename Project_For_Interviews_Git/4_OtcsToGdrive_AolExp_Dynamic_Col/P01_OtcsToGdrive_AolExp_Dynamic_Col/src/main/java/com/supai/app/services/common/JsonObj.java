package com.supai.app.services.common;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class JsonObj {
	private final Logger log = LoggerFactory.getLogger(getClass());// if log variable is static then use ClassName.class insted of getClass()

	private final ObjectMapper objectMapper;

	public JsonNode getJson(String jsonString) {
		try {
			JsonNode res =  objectMapper.readTree(jsonString); // Parse the string into JsonNode
			return res;
		} catch (Exception e) {
			log.error("Failed to parse JSON" + e);
			return null; // Return null if parsing fails
		}
	}
	
	public String getFormatedJsonString(JsonNode node) {
		try {
		    String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
		    return prettyJson;
		} catch (Exception e) {
		    log.error("Faild: Failed to format JsonNode", e);
		    return null;
		}
	}
	
	public JsonNode getResultFromOtcsResponse(ResponseEntity<String> response) throws Exception{
		// TODO Auto-generated method stub
		JsonNode jsonBody = getJson(response.getBody());
		JsonNode result = jsonBody.path("results");
		JsonNode jsonResult = (result.isMissingNode() || result.isNull()) ? null : result;
		if (jsonResult == null) {
			log.info("Exception : fetching 'results' from response body of search is faild");
			throw new IllegalStateException("Missing or null 'results' in OTCS response"); // run time exception so no need to declear in method signature
		}
		return jsonResult;
	}
	
	public Map<String, Object> convertSimpleJsonNodeToMap(JsonNode jsonNode) {
        return objectMapper.convertValue(jsonNode, Map.class);
    }
}
