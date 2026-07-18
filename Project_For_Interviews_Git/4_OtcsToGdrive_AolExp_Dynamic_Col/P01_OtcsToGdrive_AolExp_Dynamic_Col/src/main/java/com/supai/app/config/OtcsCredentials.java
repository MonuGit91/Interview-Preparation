package com.supai.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "otcs.credentials")
public class OtcsCredentials {
    private String authApi;
    private String username;
    private String password;
    private String otcsticket;
}