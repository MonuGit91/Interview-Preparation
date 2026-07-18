//package com.supai.app.services.otcs;
//
//import java.util.concurrent.Callable;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Component;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//
//import lombok.RequiredArgsConstructor;
//
////@Component
////@RequiredArgsConstructor
////public class CallOtcsApi {
////	private static final Logger log = LoggerFactory.getLogger(CallOtcsApi.class);
////	private final ObjectMapper objectMapper;
////	private final OtcsTicket otcsTicket;
////
////	public <T> ResponseEntity<T> callWithRetry(Callable<ResponseEntity<T>> callable, Class<T> responseType) {
////		Exception exception = null;
////		int numberOfAttempt = 0;
////		while (++numberOfAttempt <= 2) {
////			try {
////				ResponseEntity<T> response = callable.call();
////				return response;
////			} catch (Exception exc) {
////				exception = exc;
////				if (!exception.getMessage().contains("401 Unauthorized"))
////					break;
////				// control come here that means it is "401 Unauthorized"
////				if (numberOfAttempt == 1) {
////					log.info("updating OTCS ticket....");
////					otcsTicket.handleOtcsToken();
////					log.info("OTCS ticket updated.");
////				} else if (numberOfAttempt == 2) {
////					log.info("error: {}", exc.getMessage());
////					ResponseEntity<T> responseEntity = getErrorRequestEntity("401 Unauthorized",
////							HttpStatus.UNAUTHORIZED, exception, responseType);
////					return responseEntity;
////				}
////			}
////		}
////		ResponseEntity<T> responseEntity = getErrorRequestEntity("Bad Request", HttpStatus.BAD_REQUEST, exception, responseType);
////		log.info("Error: {} ", exception.getMessage());
////		return responseEntity;
////	}
////	
////
////	@SuppressWarnings("unchecked")
////	private <T> ResponseEntity<T> getErrorRequestEntity(String statusMsg, HttpStatus statusCode, Exception exc, Class<T> responseType) {
////	    ObjectNode objectNode = objectMapper.createObjectNode();
////	    objectNode.put("error", statusMsg);
////	    objectNode.put("Exception", exc.getMessage());
////
////	    if (responseType.equals(String.class)) {
////	        // Handle String response
////	        return (ResponseEntity<T>) ResponseEntity.status(statusCode).body(objectNode.toString());
////	    } else if (responseType.equals(byte[].class)) {
////	        // Handle byte[] response
////	        return (ResponseEntity<T>) ResponseEntity.status(statusCode).body(objectNode.toString().getBytes());
////	    } else {
////	    	log.info("Error: \"UnexpectedClass\" - Can not build Exact error ResponseEntity");
////	    	return (ResponseEntity<T>) ResponseEntity.status(statusCode).body(null);
////	    }
////	}
////	
////	public <T> boolean isUnExpectedResponse(ResponseEntity<T> response) {
////		return (response.getStatusCode().equals(HttpStatus.BAD_REQUEST) || response.getStatusCode().equals(HttpStatus.UNAUTHORIZED));
////	}
////	
////}
////
////
//
//@Component
//@RequiredArgsConstructor
//public class CallOtcsApi {
//	private static final Logger log = LoggerFactory.getLogger(CallOtcsApi.class);
//	private final ObjectMapper objectMapper;
//	private final OtcsTicket otcsTicket;
//
//	public <T> ResponseEntity<T> callWithRetry(Callable<ResponseEntity<T>> callable, Class<T> responseType) {
//
//		Exception exception = null;
//		int numberOfAttempt = 0;
//		while (++numberOfAttempt <= 2) {
//			try {
//				ResponseEntity<T> response = callable.call();
//				return response;
//			} catch (Exception exc) {
//				exception = exc;
//				if (!exception.getMessage().toLowerCase().contains("unauthorized")) {
//					break;
//				}
//				// control come here that means it is "Unauthorized"
//				if (numberOfAttempt == 1) {
//					log.info("updating OTCS ticket...");
//					otcsTicket.handleOtcsToken();
//					log.info("OK: OTCS ticket updated.");
//				} else if (numberOfAttempt == 2) {
//					log.error("{}", exc.getMessage());
//					ResponseEntity<T> responseEntity = getErrorRequestEntity("Unauthorized", HttpStatus.UNAUTHORIZED,
//							exception, responseType);
//					return responseEntity;
//				}
//			}
//		}
//
//		ResponseEntity<T> responseEntity = getErrorRequestEntity("Bad Request", HttpStatus.BAD_REQUEST, exception,
//				responseType);
//		if (!exception.getMessage().contains("is not a category on node")) {
//			log.error(" {} ", exception.getMessage());
//		}
//		return responseEntity;
//	}
//
//	@SuppressWarnings("unchecked")
//	private <T> ResponseEntity<T> getErrorRequestEntity(String statusMsg, HttpStatus statusCode, Exception exc,
//			Class<T> responseType) {
//
//		ObjectNode objectNode = objectMapper.createObjectNode();
//		objectNode.put("error", statusMsg);
//		objectNode.put("Exception", exc.getMessage());
//
//		if (responseType.equals(String.class)) {
//			// Handle String response
//			return (ResponseEntity<T>) ResponseEntity.status(statusCode).body(objectNode.toString());
//		} else if (responseType.equals(byte[].class)) {
//			// Handle byte[] response
//			return (ResponseEntity<T>) ResponseEntity.status(statusCode).body(objectNode.toString().getBytes());
//		} else if (responseType.equals(JsonNode.class)) {
//			// Handle JsonNode response
//			return (ResponseEntity<T>) ResponseEntity.status(statusCode).body(objectNode);
//		} else {
//			log.error("Error: \"UnexpectedClass\" - Can not build Exact error ResponseEntity");
//			return (ResponseEntity<T>) ResponseEntity.status(statusCode).body(null);
//		}
//	}
//
//	public <T> boolean isUnExpectedResponse(ResponseEntity<T> response) {
//		return !response.getStatusCode().is2xxSuccessful();
////		return (response.getStatusCode().equals(HttpStatus.BAD_REQUEST)
////				|| response.getStatusCode().equals(HttpStatus.UNAUTHORIZED));
//	}
//
//}
