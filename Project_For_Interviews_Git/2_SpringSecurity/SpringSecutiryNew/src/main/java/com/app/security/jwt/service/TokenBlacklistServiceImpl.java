package com.app.security.jwt.service;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.security.jwt.model.BlacklistedToken;
import com.app.security.jwt.repository.BlacklistedTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of TokenBlacklistService using a database
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final BlacklistedTokenRepository repository;

    @Override
    @Transactional
    public void blacklistToken(String token, long durationMs) {
        Instant expiryDate = Instant.now().plusMillis(durationMs);
        
        BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                .token(token)
                .expiryDate(expiryDate)
                .build();
        
        repository.save(blacklistedToken);
        log.info("Token blacklisted until {}", expiryDate);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return repository.findByToken(token).isPresent();
    }
    
    /**
     * Scheduled task to clean up expired tokens from the blacklist
     * Runs every hour (3600000 ms)
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Running cleanup task for expired blacklisted tokens...");
        repository.deleteByExpiryDateBefore(Instant.now());
    }
}
