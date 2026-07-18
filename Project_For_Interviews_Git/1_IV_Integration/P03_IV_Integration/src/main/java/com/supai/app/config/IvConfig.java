package com.supai.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "iv")
public class IvConfig {
    private Base base;
    private Api api;

    @Data
    public static class Base {
        private String pub;
        private String art;
        public String pubValue;
        public String merge;
        public String markup;
    }

    @Data
    public static class Api {
        private String pub;
        private String publication;
        private String status;
        private String download;
        private String merge;
        private String graphQl;
        private String graphQlFollowup;
    }
}