package com.supai.app.exceptions;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supai.app.services.common.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final JsonUtil jsonUtil;

    // This catches your custom OkHttp errors
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<JsonNode> handleExternalApiError(ExternalApiException ex) {
        JsonNode errorBody = ex.getErrorBody();
        ObjectNode responseNode;

        if (errorBody != null && errorBody.isObject()) {
            responseNode = ((ObjectNode) errorBody).deepCopy();
        } else {
            responseNode = jsonUtil.objectMapper.createObjectNode();
            responseNode.put("Error", errorBody != null ? errorBody.asText() : "Unknown Error");
        }

        responseNode.put("failedApi", ex.getApiContext());
        responseNode.put("failedUrl", ex.getUrl());

        log.error("{}", ex.getMessage());
        return ResponseEntity
                .status(ex.getStatusCode()) // Postman will show the correct status
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseNode);
    }

    @ExceptionHandler(BannerValidationException.class)
    public ResponseEntity<JsonNode> handleBannerValidationError(BannerValidationException ex) {
        log.error("{}", ex.getMessage());
        return ResponseEntity
                .status(400) // Bad Request
                .contentType(MediaType.APPLICATION_JSON)
                .body(ex.getErrorBody());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<JsonNode> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("{}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // Wrap in standard error structure
        Map<String, String> map = new HashMap<>();
        map.put("Error", "Validation Failed");
        map.put("details", jsonUtil.mapToJsonString(errors));

        return ResponseEntity.status(400).body(jsonUtil.mapToJsonNode(map));
    }

    // This catches everything else (NullPointer, etc.) so the app doesn't crash
    // with HTML
    @ExceptionHandler(Exception.class)
    public ResponseEntity<JsonNode> handleGeneralError(Exception ex) {
        log.error("{}", ex.getMessage());
        Map<String, String> map = new HashMap<>();
        map.put("Error", "Internal Server Error");
        map.put("message", ex.getMessage());
        return ResponseEntity
                .status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonUtil.mapToJsonNode(map));
    }
}