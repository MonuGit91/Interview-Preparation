package com.supai.app.services.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JsonUtil {
	public ObjectMapper objectMapper = new ObjectMapper();

	/**
     * Converts any Java Object to a JSON String
     */
    public <T> String classToJson(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            // Log the error and handle according to your project's strategy
            throw new RuntimeException("Error converting object to JSON", e);
        }
    }
 
    /**
     * Converts a JSON String back to a Java Object
     */
    public <T> T jsonToDto(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to object", e);
        }
    }
    
    public <T> T jsonNodeToDto(JsonNode jsonNode, Class<T> clazz) {
        try {
            return objectMapper.treeToValue(jsonNode, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to object", e);
        }
    }
	

	public JsonNode add(JsonNode finalNode, String jsonString) {
		try {
			JsonNode newNode = objectMapper.readTree(jsonString); // Parse the input string into JsonNode

			if (finalNode.isObject() && newNode.isObject()) {
				ObjectNode resultNode = ((ObjectNode) finalNode).deepCopy(); // Avoid mutating original
				resultNode.setAll((ObjectNode) newNode); // Merge
				return resultNode;
			} else {
				log.warn("Cannot merge: Both nodes must be JSON objects. Got finalNode={}, newNode={}",
						finalNode.getNodeType(), newNode.getNodeType());
				return finalNode;
			}
		} catch (Exception e) {
			log.warn("Failed to parse or merge JSON: {}", e.getMessage());
			return finalNode;
		}
	}

	public String mapToJsonString(Map<String, String> map) {
		try {
			return new ObjectMapper().writeValueAsString(map);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public JsonNode addAfter(JsonNode finalNode, String key, String value, String addAfter) {
		try {
			ObjectNode orderedMetadata = new ObjectMapper().createObjectNode();
			boolean added = false;

			Iterator<Map.Entry<String, JsonNode>> fields = finalNode.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = fields.next();
				orderedMetadata.set(entry.getKey(), entry.getValue());

				if (!added && entry.getKey().equals(addAfter)) {
					orderedMetadata.put(key, value);
					added = true;
				}
			}

			return orderedMetadata;

		} catch (Exception e) {
			log.warn("Failed to parse or merge JSON: {}", e.getMessage());
			return finalNode;
		}
	}
	public JsonNode mapToJsonNode(Map<String, String> map) {
		return getJson(mapToJsonString(map));
	}

	public JsonNode getJson(String jsonString) {
		try {
			JsonNode res = objectMapper.readTree(jsonString); // Parse the string into JsonNode
			return res;
		} catch (Exception e) {
			log.error("Failed to parse JSON : {}" + e);
			JsonNode errorNode = getJson("invalidJson", e.getMessage());
			throw new RuntimeException(errorNode.asText());
		}
	}

	public JsonNode makeOrDefaultJson(String optionalKey, String jsonString) {
		try {
			JsonNode res = objectMapper.readTree(jsonString); // Parse the string into JsonNode
			return res;
		} catch (Exception e) {
			Map<String, String> map = new HashMap<>();
			map.put(optionalKey, jsonString);
			return objectMapper.valueToTree(map);
		}
	}

	public JsonNode getJson(String key, String val) {
		Map<String, String> map = new HashMap<>();
		map.put(key, val);
		return new ObjectMapper().valueToTree(map);
	}

	public String getFormatedJsonString(JsonNode node) {
		try {
			String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
			return prettyJson;
		} catch (Exception e) {
			log.error("Failed: Failed to format JsonNode {}", e);
			return null;
		}
	}

	public JsonNode getResultFromOtcsResponse(ResponseEntity<String> response) throws Exception {
		// TODO Auto-generated method stub
		JsonNode jsonBody = getJson(response.getBody());
		JsonNode result = jsonBody.path("results");
		JsonNode jsonResult = (result.isMissingNode() || result.isNull()) ? null : result;
		if (jsonResult == null) {
			log.error("Exception : fetching 'results' from response body of search is faild");
			throw new IllegalStateException("Missing or null 'results' in OTCS response");
		}
		return jsonResult;
	}

	public JsonNode getJson(String key, String msg, String jsonStr) {
		ObjectNode objectNode = objectMapper.createObjectNode();
		objectNode.put(key, msg);

		if (jsonStr != null) {
			int start = jsonStr.indexOf("{");
			int end = jsonStr.lastIndexOf("}");

			if (start != -1 && end != -1 && end > start) {
				// prefix before JSON
				String prefix = jsonStr.substring(0, start).trim();
				// valid JSON part
				String jsonPart = jsonStr.substring(start, end + 1).replaceAll("[\\r\\n]+", " ").replace("<EOL>", " ").trim();

				if (!prefix.isEmpty()) {
				    // remove any trailing quote
				    if (prefix.endsWith("\"")) {
				        prefix = prefix.substring(0, prefix.length() - 1).trim();
				    }
				    objectNode.put("status", prefix);
				}

				try {
					JsonNode details = objectMapper
							.readTree(jsonPart.replaceAll("[\\r\\n]+", " ").replace("<EOL>", " "));
					objectNode.set("default", details);
				} catch (Exception e) {
					objectNode.put("default", jsonPart); // fallback if not JSON
				}
			} else {
				// no JSON found, just put raw string
				objectNode.put("default", jsonStr);
			}
		}

		return objectNode;
	}

}
