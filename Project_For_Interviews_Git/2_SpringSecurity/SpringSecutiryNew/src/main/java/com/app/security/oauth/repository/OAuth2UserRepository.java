package com.app.security.oauth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.security.oauth.model.OAuth2User;

/**
 * Repository for OAuth2 user information
 */
@Repository
public interface OAuth2UserRepository extends JpaRepository<OAuth2User, Long> {

    Optional<OAuth2User> findByEmailAndProvider(String email, String provider);
}
