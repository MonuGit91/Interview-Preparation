package com.app.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User entity matching Spring Security's default schema
 * This entity represents the users table in the database
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    
    @Id
    @Column(name = "username", nullable = false, unique = true, columnDefinition = "citext")
    private String username;
    
    @Column(name = "password", nullable = false, length = 500)
    private String password;
    
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @jakarta.persistence.ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @jakarta.persistence.CollectionTable(
        name = "authorities", 
        joinColumns = @jakarta.persistence.JoinColumn(name = "username", columnDefinition = "citext")
    )
    @jakarta.persistence.Column(name = "authority", columnDefinition = "citext")
    private java.util.List<String> authorities;
}

