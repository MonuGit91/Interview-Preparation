package com.supai.app.exceptions;

import com.fasterxml.jackson.databind.JsonNode;

public class BannerValidationException extends RuntimeException {
    private final JsonNode errorBody;

    public BannerValidationException(JsonNode errorBody) {
        super("Banner Validation Failed");
        this.errorBody = errorBody;
    }

    public JsonNode getErrorBody() {
        return errorBody;
    }
}
