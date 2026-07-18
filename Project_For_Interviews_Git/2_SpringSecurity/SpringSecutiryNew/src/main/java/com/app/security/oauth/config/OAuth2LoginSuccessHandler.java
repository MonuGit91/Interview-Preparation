package com.app.security.oauth.config;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.app.security.oauth.service.OAuth2UserService;
import com.app.security.refreshtoken.model.RefreshToken;
import com.app.security.refreshtoken.service.RefreshTokenService;
import com.app.security.jwt.service.JwtService;

import lombok.RequiredArgsConstructor;

/**
 * OAuth2 Login Success Handler
 * Generates JWT and redirects to success endpoint
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

        @org.springframework.beans.factory.annotation.Value("${spring.app.frontend-url}")
        private String frontendUrl;

        private final OAuth2UserService oauth2UserService;
        private final RefreshTokenService refreshTokenService;
        private final JwtService jwtService;

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                        Authentication authentication) throws ServletException, IOException {

                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

                // Process user in DB and get UserDetails
                UserDetails userDetails = oauth2UserService.processUser(oauth2User);

                // Generate JWT
                String jwtToken = jwtService.generateTokenFromUsername(userDetails, "GOOGLE");

                // Generate Refresh Token
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername(), "GOOGLE");

                // Create HttpOnly Cookie
                jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("refresh_token",
                                refreshToken.getToken());
                cookie.setHttpOnly(true);
                cookie.setSecure(true); // Set to true since we are likely using HTTPS or should be
                cookie.setPath("/");
                cookie.setMaxAge((int) (60L * 60L * 24L * 30L)); // 30 days
                response.addCookie(cookie);

                // Redirect to Frontend
                String targetUrl = frontendUrl + "?token=" + jwtToken + "&refreshToken=" + refreshToken.getToken();
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
}
