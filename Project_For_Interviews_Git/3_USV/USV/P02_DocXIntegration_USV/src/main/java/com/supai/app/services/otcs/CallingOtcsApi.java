package com.supai.app.services.otcs;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CallingOtcsApi {
	private static final Logger log = LoggerFactory.getLogger(CallingOtcsApi.class);
	private final ObjectMapper objectMapper;
	private final JsonObj jsonObj;

	public <T> ResponseEntity<T> callWithRetry(Callable<ResponseEntity<T>> callable, Class<T> responseType) {

		Exception exception = null;
		int numberOfAttempt = 0;
		while (++numberOfAttempt <= 2) {
			try {
				ResponseEntity<T> response = callable.call();
				return response;
			} catch (Exception exc) {
				exception = exc;
				log.error("{}", exc.getMessage());
				if (exception.getMessage().toLowerCase().contains("unauthorized")) {
					ResponseEntity<T> responseEntity = getErrorRequestEntity("Unauthorized", HttpStatus.UNAUTHORIZED,
							exception, responseType);
					return responseEntity;
				}
				ResponseEntity<T> responseEntity = getErrorRequestEntity("Bad Request", HttpStatus.BAD_REQUEST,
						exception, responseType);
				return responseEntity;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> ResponseEntity<T> getErrorRequestEntity(String statusMsg, HttpStatus statusCode, Exception exc,
			Class<T> responseType) {

		ObjectNode objectNode = objectMapper.createObjectNode();
		objectNode.put("error", statusMsg);
		objectNode.put("Exception", exc.getMessage());

		if (responseType.equals(String.class)) {
			// Handle String response
			return (ResponseEntity<T>) ResponseEntity.status(statusCode).body(objectNode.toString());
		} else if (responseType.equals(byte[].class)) {
			// Handle byte[] response
			return (ResponseEntity<T>) ResponseEntity.status(statusCode).body(objectNode.toString().getBytes());
		} else if (responseType.equals(JsonNode.class)) {
			// Handle JsonNode response
			return (ResponseEntity<T>) ResponseEntity.status(statusCode).body(objectNode);
		} else {
			JsonNode error = jsonObj.getJson("Error", "UnexpectedClass - Can not build Exact error ResponseEntity");
			log.error(error.toString());
			return (ResponseEntity<T>) ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
		}
	}

	public <T> boolean isUnExpectedResponse(ResponseEntity<T> response) {
		return (response.getStatusCode().equals(HttpStatus.BAD_REQUEST)
				|| response.getStatusCode().equals(HttpStatus.UNAUTHORIZED));
	}

}
