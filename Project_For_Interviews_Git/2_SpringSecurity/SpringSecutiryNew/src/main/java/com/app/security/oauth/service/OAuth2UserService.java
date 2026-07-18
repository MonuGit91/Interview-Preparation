package com.app.security.oauth.service;

import java.util.Collections;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.app.security.oauth.repository.OAuth2UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * OAuth2 User Service Handles OAuth2 user authentication and processing
 */
@Service
@RequiredArgsConstructor
public class OAuth2UserService {

    private final OAuth2UserRepository oauth2UserRepository;

    /**
     * Reverting strict sync logic.
     * We will NOT create a user in the main table anymore.
     * We will purely rely on the 'oauth2_users' table.
     */
    public UserDetails processUser(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        String providerId = oAuth2User.getAttribute("sub");
        String provider = "GOOGLE";

        Optional<com.app.security.oauth.model.OAuth2User> userOptional = oauth2UserRepository
                .findByEmailAndProvider(email, provider);

        com.app.security.oauth.model.OAuth2User userEntity;

        if (userOptional.isPresent()) {
            userEntity = userOptional.get();
            userEntity.setName(name);
            userEntity.setProviderId(providerId);
            oauth2UserRepository.save(userEntity);
        } else {
            userEntity = com.app.security.oauth.model.OAuth2User.builder()
                    .email(email)
                    .name(name)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            oauth2UserRepository.save(userEntity);
        }

        return new User(email, "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public UserDetails loadUserByEmail(String email) {
        com.app.security.oauth.model.OAuth2User user = oauth2UserRepository.findByEmailAndProvider(email, "GOOGLE")
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "OAuth2 User not found: " + email));

        return new User(user.getEmail(), "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
