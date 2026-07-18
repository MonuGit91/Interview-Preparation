package com.supai.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "otcs") // Start at 'otcs' to capture both base-url and api
public class OtcsApi {
    public Api api; // Maps to otcs.api
    public Base base; // Maps to otcs.base
    private String qrCode;

    @Data
    public static class Base {
        private String common;
    }

    @Data
    public static class Api {
        private String authApi;
        private String createOrCopyNode; //create-or-copy-node
        private String getDoc;
        private String searchDocs;
        private String categoryInfo;
        private String nodeProperty;
        private String childNodeProperty;
        private String userInfo;
        private String addOrGetVersion;
        private String reserveToggle;
        private String removeCategory;
        private String addCategory;
        private String updateCategory;
    }
}