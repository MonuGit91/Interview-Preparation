package com.app.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for user registration response
 */
@Data
@AllArgsConstructor
@Builder
public class RegisterResponse {
    
    private String message;
    private boolean success;
    private String username;
}

