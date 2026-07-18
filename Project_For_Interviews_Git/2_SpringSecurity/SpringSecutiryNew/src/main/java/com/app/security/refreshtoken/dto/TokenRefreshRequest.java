package com.app.security.refreshtoken.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for refreshing an access token
 */
@Data
public class TokenRefreshRequest {
    
    @NotBlank
    private String refreshToken;
}
