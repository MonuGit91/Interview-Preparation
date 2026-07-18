package com.app.security.oauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * OAuth2 Configuration
 * Configures OAuth2 client registration and authorization using Customizer
 * pattern
 */
@Configuration
@RequiredArgsConstructor
public class OAuth2Config {

    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;



    @Bean
    public Customizer<OAuth2LoginConfigurer<HttpSecurity>> oauth2LoginCustomizer(
            org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository) {
        return oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                        .authorizationRequestResolver(authorizationRequestResolver(clientRegistrationRepository)))
                .successHandler(oauth2LoginSuccessHandler);
    }

    private org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository) {

        org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver resolver = 
                new org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");

        resolver.setAuthorizationRequestCustomizer(
                authorizationRequest -> authorizationRequest
                        .additionalParameters(params -> params.put("prompt", "select_account")));

        return resolver;
}
}
