package com.app.security.jwt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration properties for JWT token management
 * Properties are loaded from application.yml under spring.app
 */
@Component
@Data
@ConfigurationProperties(prefix = "spring.app")
public class JwtProperties {
    
    /**
     * Secret key for signing JWT tokens (should be at least 256 bits)
     */
    private String jwtSecret;
    
    /**
     * JWT token expiration time in milliseconds
     */
    private long jwtExpirationMs;
    
    /*
     * JWT refresh token expiration time in milliseconds
     * TODO: Implement refresh token mechanism
     */
    private long jwtRefreshExpirationMs = 86400000; // 24 hours default
    
    /**
     * Absolute expiration for refresh tokens (default 7 days)
     */
    private long jwtRefreshAbsoluteExpirationMs = 604800000; // 7 days default (ms)
}

