package com.supai.app.exceptions;

import com.fasterxml.jackson.databind.JsonNode;

public class ExternalApiException extends RuntimeException {
    private final int statusCode;
    private final JsonNode errorBody; // Changed to JsonNode
    private final String apiContext;
    private final String url;

    public ExternalApiException(int statusCode, JsonNode errorBody, String apiContext, String url) {
        super("External API Error in " + apiContext + " (" + url + "): " + statusCode);
        this.statusCode = statusCode;
        this.errorBody = errorBody;
        this.apiContext = apiContext;
        this.url = url;
    }

    public ExternalApiException(int statusCode, JsonNode errorBody, String apiContext) {
        this(statusCode, errorBody, apiContext, "Unknown URL");
    }

    public ExternalApiException(int statusCode, JsonNode errorBody) {
        this(statusCode, errorBody, "Unknown API", "Unknown URL");
    }



	public int getStatusCode() {
        return statusCode;
    }

    public JsonNode getErrorBody() {
        return errorBody;
    }

    public String getApiContext() {
        return apiContext;
    }

    public String getUrl() {
        return url;
    }
}