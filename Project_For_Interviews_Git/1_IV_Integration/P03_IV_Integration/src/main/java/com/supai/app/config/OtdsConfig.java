package com.supai.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "otds")
public class OtdsConfig {
    private String base;
    private Api api;
    private String userId;
    private String password;
 
    @Data
    public static class Api {
        private String impersonateUser;
        private String authenticate;
    }
}
