package com.app.security.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for OAuth2 login response
 * TODO: Implement OAuth2 login response DTO when needed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2LoginResponse {
    
    private String token;
    private String tokenType;
    private String provider;
    private String message;
}

