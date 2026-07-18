package com.supai.app.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "spring.datasource")
public class DbCredentials {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
}