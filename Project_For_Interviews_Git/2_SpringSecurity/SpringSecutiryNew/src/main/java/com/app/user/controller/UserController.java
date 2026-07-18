package com.app.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for user authentication and management endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class UserController {
    /**
     * Public endpoint accessible to all authenticated users
     * @return greeting message
     */
    @GetMapping("/common")
    public ResponseEntity<String> commonEndpoint() {
        return ResponseEntity.ok("Common Endpoint - Accessible to all authenticated users");
    }
    
    /**
     * User-only endpoint
     * @return greeting message
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/hello")
    public ResponseEntity<String> userEndpoint() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok("Hello, " + username + "! (User Role)");
    }
    
    /**
     * Admin-only endpoint
     * @return greeting message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/hello")
    public ResponseEntity<String> adminEndpoint() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok("Hello, " + username + "! (Admin Role)");
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private class ErrorResponse {
        private final String message;
        private final boolean status;
    }
}