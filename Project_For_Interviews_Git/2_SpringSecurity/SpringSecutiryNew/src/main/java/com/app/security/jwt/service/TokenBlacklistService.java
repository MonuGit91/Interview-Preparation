package com.app.security.jwt.service;

/**
 * Service interface for handling Token Blacklisting (Revocation)
 */
public interface TokenBlacklistService {

    /**
     * Adds a token to the blacklist with a specific expiration time
     * @param token the JWT token to blacklist
     * @param durationMs duration in milliseconds until the token naturally expires
     */
    void blacklistToken(String token, long durationMs);

    /**
     * Checks if a token is currently blacklisted
     * @param token the JWT token to check
     * @return true if blacklisted, false otherwise
     */
    boolean isBlacklisted(String token);
}
