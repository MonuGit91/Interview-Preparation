package com.app.security.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for OAuth2 login request
 * TODO: Implement OAuth2 login request DTO when needed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2LoginRequest {
    
    private String provider; // google, github, facebook, etc.
    private String authorizationCode;
}

