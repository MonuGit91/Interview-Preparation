package com.app.security.oauth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity for storing OAuth2 user information
 */
@Entity
@Table(name = "oauth2_users", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "email", "provider" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;
}
