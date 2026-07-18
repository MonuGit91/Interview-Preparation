package com.app.security.refreshtoken.service;

import java.util.Optional;

import com.app.security.refreshtoken.model.RefreshToken;

/**
 * Service interface for Refresh Token management
 */
public interface RefreshTokenService {

    Optional<RefreshToken> findByToken(String token);

    RefreshToken createRefreshToken(String username, String provider);

    RefreshToken verifyExpiration(RefreshToken token);

    int deleteByUserId(String username);

    int deleteByToken(String token);
}
