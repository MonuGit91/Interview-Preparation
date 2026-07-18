package com.app.user.service;

import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;
import com.app.user.dto.RegisterRequest;
import com.app.user.model.UserEntity;

/**
 * Interface for user-related business logic
 */
public interface UserService {
    
    /**
     * Register a new user
     * @param registerRequest the registration request
     * @return UserEntity of the created user
     */
    UserEntity registerUser(RegisterRequest registerRequest);
    
    /**
     * Check if username exists
     * @param username the username to check
     * @return true if exists, false otherwise
     */
    boolean usernameExists(String username);
    
    /**
     * Get authorities/roles for a user
     * @param userDetails the user details
     * @return list of role names
     */
    List<String> getUserRoles(UserDetails userDetails);
    
    /**
     * Get user entity by username
     * @param username the username
     * @return UserEntity
     */
    UserEntity getUserByUsername(String username);
}
