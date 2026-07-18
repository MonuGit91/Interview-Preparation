package com.app.security.jwt.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.app.security.jwt.model.BlacklistedToken;

/**
 * Repository for BlacklistedToken entity
 */
@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    
    Optional<BlacklistedToken> findByToken(String token);
    
    @Modifying
    @Query("DELETE FROM BlacklistedToken b WHERE b.expiryDate < :now")
    void deleteByExpiryDateBefore(Instant now);
}
