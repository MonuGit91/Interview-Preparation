package com.supai.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;

@Component
@ConfigurationProperties(prefix = "otcs.api")
@Data
public class OtcsApi {
    private String baseUrl;
    private String getDoc;
    private String searchDocs;
    private String categoryInfo;
    private String nodeProperty;
    private String childNodeProperty;
    private String userInfo;
    private String addVersion;
    private String reserveToggle;
    private String removeCategory;
    private String addCategory;
    private String updateCategory;

    private Auth auth;

    @Data // instead of just @Getter
    public static class Auth {
        private String authApi;
    }
}
