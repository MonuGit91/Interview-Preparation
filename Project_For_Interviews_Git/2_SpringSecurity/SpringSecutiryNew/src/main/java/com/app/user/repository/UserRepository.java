package com.app.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.user.model.UserEntity;

/**
 * Repository interface for User entity
 * Provides data access operations for user management
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    
    /**
     * Find user by username
     * @param username the username to search for
     * @return Optional containing UserEntity if found
     */
    Optional<UserEntity> findByUsername(String username);
    
    /**
     * Check if user exists by username
     * @param username the username to check
     * @return true if user exists, false otherwise
     */
    boolean existsByUsername(String username);
}

