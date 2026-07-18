package com.app.security.jwt.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Date;

/**
 * Interface for JWT token generation, validation, and parsing
 */
public interface JwtService {

    /**
     * Generate JWT token from UserDetails
     * 
     * @param userDetails the user details
     * @return JWT token string
     */
    String generateTokenFromUsername(UserDetails userDetails);

    /**
     * Generate JWT token with custom expiration
     * 
     * @param userDetails  the user details
     * @param expirationMs custom expiration in milliseconds
     * @return JWT token string
     */
    String generateTokenFromUsername(UserDetails userDetails, long expirationMs);

    /**
     * Extract JWT token from Authorization header
     * 
     * @param request the HTTP request
     * @return JWT token string or null if not found
     */
    String getJwtFromHeader(HttpServletRequest request);

    /**
     * Generate JWT token from UserDetails with provider
     * 
     * @param userDetails the user details
     * @param provider    the authentication provider (LOCAL, GOOGLE)
     * @return JWT token string
     */
    String generateTokenFromUsername(UserDetails userDetails, String provider);

    /**
     * Get provider from JWT token
     * 
     * @param token the JWT token
     * @return provider extracted from token
     */
    String getProviderFromJwtToken(String token);

    /**
     * Get username from JWT token
     * 
     * @param token the JWT token
     * @return username extracted from token
     */
    String getUsernameFromJwtToken(String token);

    /**
     * Get expiration date from JWT token
     * 
     * @param token the JWT token
     * @return expiration date or null if token is invalid
     */
    Date getExpirationDateFromToken(String token);

    /**
     * Validate JWT token
     * 
     * @param authToken the token to validate
     * @return true if token is valid, false otherwise
     */
    boolean validateJwtToken(String authToken);

    /**
     * Check if token is expired
     * 
     * @param token the JWT token
     * @return true if expired, false otherwise
     */
    boolean isTokenExpired(String token);

}
