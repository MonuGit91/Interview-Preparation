package com.app.security.refreshtoken.dto;

import lombok.Data;

/**
 * Request DTO for Logout (Revocation)
 */
@Data
public class LogoutRequest {
    
    /**
     * If provided, revokes this specific refresh token (Session Logout)
     */
    private String refreshToken;
    
    /**
     * If true (and authenticated), revokes all sessions for the user
     */
    private boolean allDevices;
}
