package com.app.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.user.dto.RegisterRequest;
import com.app.user.model.UserEntity;
import com.app.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service layer for user-related business logic
 * Handles user registration, authentication, and user details management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Load user details by username for Spring Security
     * @param username the username to load
     * @return UserDetails object
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        
        return User.builder()
                .username(userEntity.getUsername())
                .password(userEntity.getPassword())
                .disabled(!userEntity.getEnabled())
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(false)
                .roles(userEntity.getAuthorities().stream()
                        .map(role -> role.replace("ROLE_", ""))
                        .toArray(String[]::new))
                .build();
    }

    /**
     * Register a new user
     * @param registerRequest the registration request
     * @return UserEntity of the created user
     * @throws IllegalArgumentException if validation fails
     */
    @Override
    @Transactional
    public UserEntity registerUser(RegisterRequest registerRequest) {
        log.info("Registering new user: {}", registerRequest.getUsername());
        
        // Validate username
        if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        
        // Validate password
        if (registerRequest.getPassword() == null || registerRequest.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        
        // Validate password confirmation
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        
        // Check if user already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Create user entity
        UserEntity user = UserEntity.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .enabled(true)
                .authorities(java.util.Collections.singletonList("ROLE_USER"))
                .build();
        
        // Save user
        return userRepository.save(user);
    }
    
    /**
     * Check if username exists
     * @param username the username to check
     * @return true if exists, false otherwise
     */
    @Override
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * Get authorities/roles for a user
     * @param userDetails the user details
     * @return list of role names
     */
    @Override
    public List<String> getUserRoles(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }
    
    /**
     * Get user entity by username
     * @param username the username
     * @return UserEntity
     */
    @Override
    @Transactional(readOnly = true)
    public UserEntity getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}

