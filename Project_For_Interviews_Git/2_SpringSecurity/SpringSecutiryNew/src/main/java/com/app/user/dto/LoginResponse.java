package com.app.user.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for user login response containing JWT token and user details
 */
@Data
@AllArgsConstructor
@Builder
public class LoginResponse {
    
    private String username;
    private String jwtToken;
    private String refreshToken;
    private List<String> roles;
}
