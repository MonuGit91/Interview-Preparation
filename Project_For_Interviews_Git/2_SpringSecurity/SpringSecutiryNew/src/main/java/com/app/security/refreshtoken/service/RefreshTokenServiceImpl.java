package com.app.security.refreshtoken.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.security.jwt.config.JwtProperties;
import com.app.security.refreshtoken.model.RefreshToken;
import com.app.security.refreshtoken.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * Implementation of RefreshTokenService
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(String username, String provider) {

        RefreshToken refreshToken = RefreshToken.builder()
                .username(username)
                .provider(provider)
                .expiryDate(Instant.now().plusMillis(jwtProperties.getJwtRefreshExpirationMs()))
                .token(UUID.randomUUID().toString())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        // Check structural expiry (e.g. idle timeout if we were updating it)
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new com.app.security.jwt.exception.TokenRefreshException(token.getToken(),
                    "Refresh token was expired. Please make a new signin request");
        }

        // Check absolute session timeout
        // createdDate + 7 days < Now (Absolute limit) ie, If (createdDate +
        // absoluteExpiry) < now -> Expired
        if (token.getCreatedDate().plusMillis(jwtProperties.getJwtRefreshAbsoluteExpirationMs())
                .compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new com.app.security.jwt.exception.TokenRefreshException(token.getToken(),
                    "Session expired (Absolute Timeout). Please login again.");
        }

        return token;
    }

    @Override
    @Transactional
    public int deleteByToken(String token) {
        return refreshTokenRepository.deleteByToken(token);
    }

    @Override
    @Transactional
    public int deleteByUserId(String username) {
        return refreshTokenRepository.deleteByUsername(username);
    }
}
