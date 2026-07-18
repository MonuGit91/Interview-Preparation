package com.app.security.jwt.service;

import java.security.Key;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.app.security.jwt.config.JwtProperties;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Service for JWT token generation, validation, and parsing
 * Handles all JWT-related business logic
 */
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProperties jwtProperties;

    /**
     * Generate JWT token from UserDetails
     * 
     * @param userDetails the user details
     * @return JWT token string
     */
    public String generateTokenFromUsername(UserDetails userDetails) {
        // Default to LOCAL if not specified
        return generateTokenFromUsername(userDetails, "LOCAL");
    }

    /**
     * Generate JWT token from UserDetails with provider
     * 
     * @param userDetails the user details
     * @param provider    the authentication provider (LOCAL, GOOGLE)
     * @return JWT token string
     */
    public String generateTokenFromUsername(UserDetails userDetails, String provider) {
        String username = userDetails.getUsername();

        return Jwts.builder()
                .subject(username)
                .claim("provider", provider)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getJwtExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate JWT token with custom expiration
     * 
     * @param userDetails  the user details
     * @param expirationMs custom expiration in milliseconds
     * @return JWT token string
     */
    public String generateTokenFromUsername(UserDetails userDetails, long expirationMs) {
        String username = userDetails.getUsername();

        return Jwts.builder()
                .subject(username)
                .claim("provider", "LOCAL") // Default for custom expiry usually means refresh token or similar
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract JWT token from Authorization header
     * 
     * @param request the HTTP request
     * @return JWT token string or null if not found
     */
    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        logger.debug("Authorization Header: {}", bearerToken);

        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * Get provider from JWT token
     * 
     * @param token the JWT token
     * @return provider extracted from token
     */
    public String getProviderFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("provider", String.class);
    }

    /**
     * Get username from JWT token
     * 
     * @param token the JWT token
     * @return username extracted from token
     */
    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Get expiration date from JWT token
     * 
     * @param token the JWT token
     * @return expiration date or null if token is invalid
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
        } catch (Exception e) {
            logger.error("Error extracting expiration date from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is expired
     * 
     * @param token the JWT token
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }

    /**
     * Validate JWT token
     * 
     * @param authToken the token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is not supported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Get the signing key for JWT tokens
     * 
     * @return SecretKey for signing
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getJwtSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
